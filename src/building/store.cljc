(ns building.store
  "SSoT for the building site-operations-coordination actor, behind a
  `Store` protocol so the backend is a swap, not a rewrite:

    - `MemStore`     -- atom of EDN. The deterministic default for
                        dev/tests/demo (no deps).
    - `DatomicStore` -- backed by `langchain.db`, a Datomic-API-compatible
                        EAV store. Pure `.cljc`.

  Both implement the same protocol and pass the same contract
  (test/building/store_contract_test.clj).

  The primary entity is a `project` (a construction project/site). Each
  project carries:
    - `:id` / `:name` / `:jurisdiction`
    - `:hazard-flagged?` -- true once a safety hazard is flagged
    - `:inspection-reviewed?` -- true once a formal inspection review is
                                 scheduled/conducted
    - `:unresolved-hazards` -- list of unresolved hazard records

  The ledger stays append-only: 'which operation was proposed,
  approved/rejected, on what jurisdictional basis, approved by whom' is
  always a query over an immutable log."
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [langchain.db :as d]))

(defprotocol Store
  (project [s id])
  (all-projects [s])
  (hazard-already-flagged? [s project-id hazard-id])
  (project-has-unresolved-hazard? [s project-id])
  (ledger [s])
  (hazard-flag-history [s] "append-only safety hazard flagging history")
  (inspection-review-history [s] "append-only inspection review history")
  (next-hazard-sequence [s jurisdiction])
  (next-inspection-sequence [s jurisdiction])
  (commit-record! [s record] "apply a committed op's record to the SSoT")
  (append-ledger! [s fact] "append one immutable decision fact")
  (with-projects [s projects] "replace/seed the project directory"))

;; ----------------------------- demo data -----------------------------

(defn demo-data
  "A small, self-contained project set covering all operation types
  and failure modes, so the actor + tests run offline."
  []
  {:projects
   {"proj-1" {:id "proj-1" :name "Sakura Community Housing Block C"
              :jurisdiction "JPN" :status :active
              :hazard-flagged? false :inspection-reviewed? false
              :unresolved-hazards []
              :trades [{:name "steel-frame" :crew-size 8}
                       {:name "concrete-pour" :crew-size 6}]}
    "proj-2" {:id "proj-2" :name "Atlantis Waterfront Tower"
              :jurisdiction "USA" :status :active
              :hazard-flagged? false :inspection-reviewed? false
              :unresolved-hazards []
              :trades [{:name "foundation" :crew-size 10}]}
    "proj-3" {:id "proj-3" :name "鈴木ビル改修工事"
              :jurisdiction "JPN" :status :active
              :hazard-flagged? true :inspection-reviewed? false
              :unresolved-hazards [{:id "hazard-1" :type "scaffold-stability" :flagged-at "2026-07-14T10:00:00Z"}]
              :trades [{:name "scaffolding" :crew-size 4}]}
    "proj-4" {:id "proj-4" :name "Liberty Ave Apartments"
              :jurisdiction "USA" :status :active
              :hazard-flagged? false :inspection-reviewed? true
              :unresolved-hazards []
              :trades [{:name "electrical" :crew-size 5}]}}
   :hazard-flags []
   :inspection-reviews []
   :ledger []})

;; ----------------------------- shared commit logic -----------------------------

(defn- flag-hazard!
  [s project-id hazard-id]
  (let [seq-n (next-hazard-sequence s (-> (project s project-id) :jurisdiction))]
    {:result {:flag-number seq-n :hazard-id hazard-id}
     :project-patch {:hazard-flagged? true}
     :hazard-record {:hazard-id hazard-id :seq-number seq-n :flagged-at "2026-07-14T00:00:00Z"}}))

(defn- schedule-inspection!
  [s project-id]
  (let [seq-n (next-inspection-sequence s (-> (project s project-id) :jurisdiction))]
    {:result {:inspection-number seq-n}
     :project-patch {:inspection-reviewed? true}
     :inspection-record {:seq-number seq-n :scheduled-at "2026-07-14T00:00:00Z"}}))

;; ----------------------------- MemStore (default) -----------------------------

(defrecord MemStore [a]
  Store
  (project [_ id] (get-in @a [:projects id]))
  (all-projects [_] (sort-by :id (vals (:projects @a))))
  (hazard-already-flagged? [_ project-id hazard-id]
    (boolean (some #(= hazard-id %)
                   (->> (:hazard-flags @a)
                        (filter #(= project-id (:project-id %)))
                        (map :hazard-id)))))
  (project-has-unresolved-hazard? [_ project-id]
    (boolean (seq (get-in @a [:projects project-id :unresolved-hazards]))))
  (ledger [_] (:ledger @a))
  (hazard-flag-history [_] (:hazard-flags @a))
  (inspection-review-history [_] (:inspection-reviews @a))
  (next-hazard-sequence [_ jurisdiction]
    (-> @a :hazard-flags count inc))
  (next-inspection-sequence [_ jurisdiction]
    (-> @a :inspection-reviews count inc))
  (commit-record! [_ record]
    (let [{:keys [path value]} record]
      (swap! a (fn [st]
                 (case (first path)
                   :hazard (update-in st (conj [:projects] (second path))
                                      merge (:project-patch value))
                   :inspection (update-in st (conj [:projects] (second path))
                                          merge (:project-patch value))
                   st)))))
  (append-ledger! [_ fact]
    (swap! a update :ledger conj fact))
  (with-projects [_ projects]
    (swap! a assoc :projects projects)))

;; ----------------------------- constructor -----------------------------

(defn mem-store
  "Returns a new in-memory Store backed by an atom. Seed with optional
  `:data` map (defaults to `demo-data`)."
  [& [{:keys [data] :or {data (demo-data)}}]]
  (->MemStore (atom data)))
