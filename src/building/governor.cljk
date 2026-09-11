(ns building.governor
  "Building Governor -- the independent compliance layer that earns the
  Building Advisor the right to commit. This actor coordinates
  construction site operations (trade dispatch, progress logging, safety
  hazard flagging, inspection scheduling) but NEVER directly executes
  construction work and NEVER certifies a building as safe or ready for
  occupancy (both require a licensed inspector/engineer's exclusive
  authority).

  Five checks, in priority order, ALL HARD violations: a human approver
  CANNOT override them. The confidence/high-stakes gate is SOFT: it asks
  a human to look, and the human may approve.

    1. Project not registered      -- is the construction project in the
                                     registry? A proposal for an unknown
                                     project cannot proceed.
    2. Effect not :propose        -- all operations are coordination
                                     proposals only. Direct action /
                                     work authorization / certification
                                     authority is exclusive to licensed
                                     professionals.
    3. Certification attempt       -- any proposal attempting to issue
                                     building-safety certification,
                                     occupancy permits, or code
                                     compliance sign-off is PERMANENTLY
                                     BLOCKED. This actor never certifies.
    4. Unresolved safety hazard    -- a hazard flagged on this project
                                     that has not been resolved must
                                     block further operations until
                                     cleared by a human inspector/
                                     engineer.
    5. Already flagged hazard      -- do not re-flag the same hazard
                                     twice for the same project.
    6. Confidence floor /
       high-stakes gate           -- safety-hazard flagging and
                                     inspection-review scheduling always
                                     escalate to a human."
  (:require [building.facts :as facts]
            [building.store :as store]))

(def confidence-floor 0.6)

(def high-stakes
  "Operations that always require human review, even when the governor
  is clean: flagging a safety hazard on a jobsite and scheduling a
  formal building-code inspection review."
  #{:flag-safety-hazard :request-inspection-review})

;; ----------------------------- checks -----------------------------

(defn- project-not-registered-violations
  "A proposal on a project that does not exist in the registry is a
  HARD violation -- never allow operations on unknown projects."
  [{:keys [subject]} st]
  (when-not (store/project st subject)
    [{:rule :project-not-registered
      :detail (str subject " は登録済みプロジェクトとして認識されていない")}]))

(defn- effect-not-propose-violations
  "All operations are coordination proposals only. Direct action,
  work authorization, or certification is NOT allowed."
  [proposal]
  (when-not (= :propose (:effect proposal))
    [{:rule :effect-not-propose
      :detail "建築工事の現場調整は提案（:propose）のみ。直接的な作業指示や認可権は認められない"}]))

(defn- certification-attempt-violations
  "Any attempt to certify building safety, code compliance, or issue
  occupancy determination is PERMANENTLY BLOCKED. This actor never
  certifies -- that requires a licensed inspector/engineer."
  [proposal]
  (let [value (:value proposal)
        summary (:summary proposal "")]
    (when (or (some-> value :certify-safety boolean)
              (some-> value :issue-occupancy boolean)
              (some-> value :code-compliance-sign-off boolean)
              (re-find #"(?i:certif|occupancy|code.?compli|sign.?off)" summary))
      [{:rule :certification-attempt
        :detail "建築安全認証・竣工認可・適合判定の発行は許可されない。これらは認定建築士または適合判定員等の排他的権限"}])))

(defn- unresolved-safety-hazard-violations
  "An unresolved safety hazard on this project blocks further
  operations until resolved by a human inspector/engineer."
  [{:keys [subject]} st]
  (when (store/project-has-unresolved-hazard? st subject)
    [{:rule :unresolved-safety-hazard
      :detail (str subject " に未解決の安全ハザードが存在。解決まで操作は進められない")}]))

(defn- already-flagged-hazard-violations
  "Do not flag the same hazard twice for the same project."
  [{:keys [op subject]} proposal st]
  (when (= op :flag-safety-hazard)
    (let [hazard-id (get-in proposal [:value :hazard-id])]
      (when (store/hazard-already-flagged? st subject hazard-id)
        [{:rule :already-flagged-hazard
          :detail (str subject " に既に同じハザード報告済み: " hazard-id)}]))))

(defn- legal-basis-missing-violations
  "A proposal with no legal-basis citation is a HARD violation --
  never invent a jurisdiction's building-construction requirements."
  [proposal]
  (when (or (empty? (:cites proposal))
            (and (contains? (:value proposal {}) :spec-basis)
                 (nil? (get-in proposal [:value :spec-basis]))))
    [{:rule :no-legal-basis
      :detail "公式legal-basisの引用が無い提案は建築法規に基づく現場調整として扱えない"}]))

(defn check
  "Censors a Building Advisor proposal against the governor rules.
  Returns {:ok? bool :violations [..] :confidence c :escalate? bool
  :high-stakes? bool :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (project-not-registered-violations request st)
                           (effect-not-propose-violations proposal)
                           (certification-attempt-violations proposal)
                           (unresolved-safety-hazard-violations request st)
                           (already-flagged-hazard-violations request proposal st)
                           (legal-basis-missing-violations proposal)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:op request)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
