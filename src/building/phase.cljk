(ns building.phase
  "Phase 0->3 staged rollout -- the building site-operations analog of
  `construction.phase`.

    Phase 0  read-only        -- no writes, still governor-gated.
    Phase 1  assisted-intake  -- project intake allowed, every write
                                 needs human approval.
    Phase 2  assisted-ops     -- adds progress logging and trade dispatch
                                 writes, still approval.
    Phase 3  supervised auto  -- governor-clean, high-confidence
                                 `:log-progress` and `:schedule-trade-
                                 dispatch` may auto-commit.

  Safety-critical operations (`:flag-safety-hazard` and
  `:request-inspection-review`) are ABSENT from every phase's `:auto`
  set, including phase 3 -- a permanent structural fact. These always
  escalate to a human safety officer/project manager.

  Note: all operations in this actor are proposals (`:effect :propose`).
  This is not a rollout difference across phases -- it is an invariant
  enforced by the governor for all phases.")

(def read-ops  #{})
(def write-ops #{:log-progress-record :schedule-trade-dispatch
                 :flag-safety-hazard :request-inspection-review})

;; NOTE the invariant: `:flag-safety-hazard` and
;; `:request-inspection-review` are members of `write-ops` (governor-
;; gated like any write) but are NEVER members of any phase's `:auto`
;; set. These always escalate to a human.
(def phases
  "phase -> {:label .. :writes <ops allowed to write> :auto <ops allowed to
  auto-commit when governor-clean>}."
  {0 {:label "read-only"       :writes #{}                                                                    :auto #{}}
   1 {:label "assisted-intake" :writes #{}                                                                    :auto #{}}
   2 {:label "assisted-ops"    :writes #{:log-progress-record :schedule-trade-dispatch :flag-safety-hazard :request-inspection-review} :auto #{}}
   3 {:label "supervised-auto" :writes write-ops
      :auto #{:log-progress-record :schedule-trade-dispatch}}})

(def default-phase 3)

(defn gate
  "Adjust a governor disposition for the rollout phase. Returns
  {:disposition kw :reason kw|nil}.

  - a governor HOLD always stays HOLD (compliance wins).
  - a write op not yet enabled in this phase -> HOLD (:phase-disabled).
  - a write op enabled but not auto-eligible -> ESCALATE (:phase-approval),
    even if the governor was clean.
  - `:flag-safety-hazard` and `:request-inspection-review` are never
    auto-eligible at any phase, so they always escalate once the
    governor clears them (or hold if the governor doesn't)."
  [phase {:keys [op]} governor-disposition]
  (let [{:keys [writes auto]} (get phases phase (get phases default-phase))]
    (cond
      (= :hold governor-disposition)       {:disposition :hold :reason nil}
      (contains? read-ops op)              {:disposition governor-disposition :reason nil}
      (not (contains? writes op))          {:disposition :hold :reason :phase-disabled}
      (and (= :commit governor-disposition)
           (not (contains? auto op)))      {:disposition :escalate :reason :phase-approval}
      :else                                {:disposition governor-disposition :reason nil})))

(defn verdict->disposition
  "Map a Building Governor verdict to a base disposition before the
  phase gate."
  [verdict]
  (cond (:hard? verdict) :hold
        (:escalate? verdict) :escalate
        :else :commit))
