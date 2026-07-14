(ns building.phase-test
  (:require [clojure.test :refer :all]
            [building.phase :as phase]))

(deftest phases-structure-test
  (testing "all phases have required fields"
    (doseq [[ph-num ph-map] phase/phases]
      (is (string? (:label ph-map)) (str "Phase " ph-num " has :label"))
      (is (set? (:writes ph-map)) (str "Phase " ph-num " has :writes"))
      (is (set? (:auto ph-map)) (str "Phase " ph-num " has :auto")))))

(deftest gate-hold-invariant-test
  (testing "HOLD disposition stays HOLD regardless of phase"
    (let [request {:op :log-progress-record}]
      (is (= :hold (-> (phase/gate 0 request :hold) :disposition)))
      (is (= :hold (-> (phase/gate 3 request :hold) :disposition))))))

(deftest gate-phase-disabled-test
  (testing "operation not in phase :writes becomes HOLD"
    (let [request {:op :schedule-trade-dispatch}]
      (is (= :hold (-> (phase/gate 0 request :commit) :disposition))))))

(deftest gate-phase-approval-test
  (testing "operation in :writes but not :auto escalates"
    (let [request {:op :flag-safety-hazard}]
      (is (= :escalate (-> (phase/gate 2 request :commit) :disposition))))))

(deftest gate-auto-commit-test
  (testing "operation in :auto at phase 3 commits"
    (let [request {:op :log-progress-record}]
      (is (= :commit (-> (phase/gate 3 request :commit) :disposition))))))

(deftest verdict->disposition-test
  (testing "hard verdict becomes HOLD"
    (let [verdict {:hard? true :escalate? false}]
      (is (= :hold (phase/verdict->disposition verdict)))))
  (testing "escalate verdict becomes ESCALATE"
    (let [verdict {:hard? false :escalate? true}]
      (is (= :escalate (phase/verdict->disposition verdict)))))
  (testing "clean verdict becomes COMMIT"
    (let [verdict {:hard? false :escalate? false}]
      (is (= :commit (phase/verdict->disposition verdict))))))
