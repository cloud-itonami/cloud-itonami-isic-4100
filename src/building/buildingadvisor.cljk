(ns building.buildingadvisor
  "Building Advisor -- the LLM-backed proposal engine that generates
  site-operations proposals for the Building Governor to censor.

  Interface:
    - `-advise` -- takes a request, returns a proposal
    - `mock-advisor` -- deterministic dummy advisor for tests
    - `trace` -- formats a decision for audit logging")

(defprotocol Advisor
  (-advise [this store request] "return a proposal map"))

;; ----------------------------- mock advisor (default, deterministic) ---

(defrecord MockAdvisor []
  Advisor
  (-advise [_this _store request]
    {:effect :propose
     :op (:op request)
     :subject (:subject request)
     :confidence 0.85
     :cites ["building.facts/spec-basis"]
     :summary (str "Site operation proposal: " (:op request))
     :value {:spec-basis "JPN"}
     :model :mock}))

(defn mock-advisor []
  (->MockAdvisor))

(defn trace
  "Format a proposal for audit logging."
  [request proposal]
  {:t :advisor-proposal
   :op (:op request)
   :subject (:subject request)
   :confidence (:confidence proposal)
   :model (:model proposal :unknown)})
