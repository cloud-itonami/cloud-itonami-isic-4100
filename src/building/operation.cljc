(ns building.operation
  "OperationActor -- one site-operations coordination run, expressed as
  a langgraph-clj StateGraph. The advisor (Building Advisor) is sealed
  into a single node (:advise); its proposal is ALWAYS routed through
  the Building Governor (:govern) and the rollout phase gate (:decide)
  before anything commits to the SSoT.

  Everything the actor depends on is injected:
    - the Store    (MemStore today)                   - `store` arg
    - the Advisor  (mock | real LLM)                  - :advisor opt
    - the Phase    (0->3 rollout)                     - :phase in ctx

  One graph run = one site-operations proposal (intake -> advise ->
  govern -> decide -> commit | hold | approval). No unbounded inner
  loop -- each operation is auditable and checkpointed.

  Human-in-the-loop = real approval workflow:
  `interrupt-before #{:request-approval}` pauses the actor and hands the
  decision to a human operator. The approver resumes with
  `{:approval {:status :approved}}` (or :rejected).

  `:flag-safety-hazard` and `:request-inspection-review` ALWAYS reach
  the approval node when the governor is clean."
  (:require [langgraph.graph :as g]
            [langgraph.checkpoint :as cp]
            [building.buildingadvisor :as buildingadvisor]
            [building.governor :as governor]
            [building.phase :as phase]
            [building.store :as store]))

(defn- commit-fact [request context proposal]
  {:t          :committed
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :commit
   :basis      (:cites proposal)
   :summary    (:summary proposal)})

(defn- commit-record [request _context proposal]
  {:effect  (:effect proposal)
   :path    [(:op request) (:subject request)]
   :value   (or (:value proposal) {})
   :payload (:value proposal)})

(defn build
  "Compiles an OperationActor graph bound to `store` (any
  `building.store/Store`).
  opts:
    :advisor      -- a `building.buildingadvisor/Advisor` (default: mock-advisor)
    :checkpointer -- langgraph checkpointer (default: in-mem)"
  [store & [{:keys [advisor checkpointer]
             :or   {advisor      (buildingadvisor/mock-advisor)
                    checkpointer (cp/mem-checkpointer)}}]]
  (-> (g/state-graph
       {:channels
        {:request     {:default nil}
         :context     {:default nil}
         :proposal    {:default nil}
         :verdict     {:default nil}
         :disposition {:default nil}
         :record      {:default nil}
         :approval    {:default nil}
         :audit       {:reducer into :default []}}})

      (g/add-node :intake (fn [s] s))

      ;; Building Advisor inference -- proposal only.
      (g/add-node :advise
        (fn [{:keys [request]}]
          (let [p (buildingadvisor/-advise advisor store request)]
            {:proposal p :audit [(buildingadvisor/trace request p)]})))

      ;; Building Governor -- independent censor.
      (g/add-node :govern
        (fn [{:keys [request context proposal]}]
          {:verdict (governor/check request context proposal store)}))

      ;; Decide: governor disposition, then the rollout-phase gate.
      ;; HARD governor violations -> HOLD (no override).
      (g/add-node :decide
        (fn [{:keys [request context proposal verdict]}]
          (let [base (phase/verdict->disposition verdict)
                ph   (:phase context phase/default-phase)
                {:keys [disposition reason]} (phase/gate ph request base)]
            (case disposition
              :hold
              {:disposition :hold
               :audit [(cond-> (governor/hold-fact request context verdict)
                         reason (assoc :phase-reason reason :phase ph))]}

              :escalate
              {:disposition :escalate
               :audit [{:t :approval-requested
                        :op (:op request) :subject (:subject request)
                        :reason (or reason
                                    (cond (:high-stakes? verdict) :safety-critical
                                          :else :low-confidence))
                        :phase ph
                        :confidence (:confidence verdict)}]}

              :commit
              {:disposition :commit
               :record (commit-record request context proposal)}))))

      ;; Approval handoff -- paused by interrupt-before; a human resumes
      ;; with :approval. Then route commit/hold.
      (g/add-node :request-approval
        (fn [{:keys [request context proposal approval verdict]}]
          (if (= :approved (:status approval))
            {:disposition :commit
             :record (commit-record request context proposal)
             :audit [{:t :human-approved :op (:op request) :subject (:subject request)}]}
            {:disposition :hold
             :audit [{:t :human-rejected :op (:op request) :subject (:subject request)}]})))

      ;; Commit the proposal and fact to the audit ledger.
      (g/add-node :commit
        (fn [{:keys [request context proposal record disposition audit]}]
          (when record
            (store/commit-record! store record))
          (store/append-ledger! store (commit-fact request context proposal))
          {:audit audit}))

      ;; Router: governor disposition -> next node.
      (g/add-conditional-edges :decide
        (fn [{:keys [disposition]}]
          (case disposition
            :hold :hold
            :escalate :request-approval
            :commit :commit)))

      ;; Router: after approval.
      (g/add-conditional-edges :request-approval
        (fn [{:keys [disposition]}]
          (case disposition
            :hold :hold
            :commit :commit)))

      ;; Terminal nodes.
      (g/add-node :hold (fn [s] s))

      ;; Finish.
      (g/add-edge :commit :end)
      (g/add-edge :hold :end)
      (g/add-edge :intake :advise)
      (g/add-edge :advise :govern)
      (g/add-edge :govern :decide)

      (g/set-entry-point :intake)
      (g/set-finish-point :end)
      (g/compile {:checkpointer checkpointer})))
