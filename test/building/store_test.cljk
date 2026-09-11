(ns building.store-test
  (:require [clojure.test :refer :all]
            [building.store :as store]))

(deftest mem-store-test
  (testing "mem-store creates a working store"
    (let [s (store/mem-store)]
      (is (satisfies? store/Store s))))
  (testing "mem-store with demo data has projects"
    (let [s (store/mem-store)]
      (is (seq (store/all-projects s)))
      (is (= 4 (count (store/all-projects s)))))))

(deftest project-retrieval-test
  (testing "project returns a project by id"
    (let [s (store/mem-store)
          proj (store/project s "proj-1")]
      (is (= "Sakura Community Housing Block C" (:name proj)))
      (is (= "JPN" (:jurisdiction proj)))))
  (testing "project returns nil for unknown id"
    (let [s (store/mem-store)]
      (is (nil? (store/project s "unknown"))))))

(deftest hazard-tracking-test
  (testing "project-has-unresolved-hazard? detects unresolved hazards"
    (let [s (store/mem-store)]
      (is (false? (store/project-has-unresolved-hazard? s "proj-1")))
      (is (true? (store/project-has-unresolved-hazard? s "proj-3"))))))

(deftest ledger-test
  (testing "ledger is initially empty"
    (let [s (store/mem-store)]
      (is (= [] (store/ledger s)))))
  (testing "append-ledger! adds a fact"
    (let [s (store/mem-store)
          fact {:t :test :op :test}]
      (store/append-ledger! s fact)
      (is (= [fact] (store/ledger s))))))
