(ns building.governor-test
  (:require [clojure.test :refer :all]
            [building.governor :as governor]
            [building.store :as store]))

(def test-store (store/mem-store))

(deftest check-clean-test
  (testing "check returns verdicts with required fields"
    (let [request {:op :log-progress-record :subject "proj-1"}
          context {:actor-id "test-actor"}
          proposal {:effect :propose :op :log-progress-record :confidence 0.8 :cites ["building.facts/spec-basis"] :value {}}
          result (governor/check request context proposal test-store)]
      (is (contains? result :ok?))
      (is (contains? result :violations))
      (is (contains? result :confidence))
      (is (contains? result :hard?))
      (is (contains? result :escalate?))
      (is (= 0.8 (:confidence result))))))

(deftest check-certification-attempt-test
  (testing "check rejects proposals attempting certification"
    (let [request {:op :log-progress-record :subject "proj-1"}
          context {:actor-id "test-actor"}
          proposal {:effect :propose :op :log-progress-record :confidence 0.9 :cites ["test"] :value {:certify-safety true} :summary ""}
          result (governor/check request context proposal test-store)]
      (is (= true (:hard? result)))
      (is (seq (:violations result))))))

(deftest check-effect-not-propose-test
  (testing "check rejects non-propose effects"
    (let [request {:op :log-progress-record :subject "proj-1"}
          context {:actor-id "test-actor"}
          proposal {:effect :direct-action :op :log-progress-record :confidence 0.9 :cites ["test"] :value {}}
          result (governor/check request context proposal test-store)]
      (is (= true (:hard? result)))
      (is (seq (:violations result))))))

(deftest high-stakes-test
  (testing "flag-safety-hazard is high-stakes"
    (is (contains? governor/high-stakes :flag-safety-hazard)))
  (testing "request-inspection-review is high-stakes"
    (is (contains? governor/high-stakes :request-inspection-review))))

(deftest check-unresolved-hazard-test
  (testing "check detects unresolved hazards on a project"
    (let [request {:op :schedule-trade-dispatch :subject "proj-3"}
          context {:actor-id "test"}
          proposal {:effect :propose :confidence 0.9 :cites ["test"] :value {}}
          result (governor/check request context proposal test-store)]
      (is (= true (:hard? result)))
      (is (seq (:violations result))))))

(deftest check-unknown-project-test
  (testing "check rejects operations on unknown projects"
    (let [request {:op :log-progress-record :subject "unknown-project"}
          context {:actor-id "test"}
          proposal {:effect :propose :confidence 0.9 :cites ["test"] :value {}}
          result (governor/check request context proposal test-store)]
      (is (= true (:hard? result)))
      (is (seq (:violations result))))))

(deftest check-safety-hazard-escalates-test
  (testing "flag-safety-hazard operation escalates (high-stakes)"
    (let [request {:op :flag-safety-hazard :subject "proj-1"}
          context {:actor-id "test"}
          proposal {:effect :propose :confidence 0.9 :cites ["test"] :value {:hazard-id "h1"}}
          result (governor/check request context proposal test-store)]
      (is (= true (:escalate? result)))
      (is (= true (:high-stakes? result))))))

(deftest confidence-floor-test
  (testing "low confidence escalates even if otherwise clean"
    (let [request {:op :log-progress-record :subject "proj-1"}
          context {:actor-id "test"}
          proposal {:effect :propose :confidence 0.3 :cites ["test"] :value {}}
          result (governor/check request context proposal test-store)]
      (is (= true (:escalate? result))))))
