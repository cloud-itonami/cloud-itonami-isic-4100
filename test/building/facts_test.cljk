(ns building.facts-test
  (:require [clojure.test :refer :all]
            [building.facts :as facts]))

(deftest coverage-test
  (testing "coverage returns expected results"
    (let [cov (facts/coverage)]
      (is (= 4 (:covered cov)))
      (is (= 4 (:requested cov)))
      (is (= ["DEU" "GBR" "JPN" "USA"] (:covered-jurisdictions cov))))))

(deftest spec-basis-test
  (testing "spec-basis returns catalog entry"
    (let [jpn (facts/spec-basis "JPN")]
      (is (= "Japan" (:name jpn)))
      (is (contains? jpn :legal-basis))
      (is (contains? jpn :provenance))))
  (testing "spec-basis returns GBR catalog entry"
    (let [gbr (facts/spec-basis "GBR")]
      (is (= "United Kingdom (England building-control framework)" (:name gbr)))
      (is (contains? gbr :owner-authority))
      (is (contains? gbr :legal-basis))
      (is (contains? gbr :provenance))
      (is (re-find #"Building Safety Act 2022" (:legal-basis gbr)))))
  (testing "spec-basis returns nil for unknown jurisdiction"
    (is (nil? (facts/spec-basis "ZZZ")))))

(deftest catalog-structure-test
  (testing "all catalog entries have required fields"
    (doseq [[iso3 entry] facts/catalog]
      (is (string? (:name entry)) (str iso3 " has :name"))
      (is (string? (:legal-basis entry)) (str iso3 " has :legal-basis"))
      (is (string? (:provenance entry)) (str iso3 " has :provenance"))
      (is (string? (:owner-authority entry)) (str iso3 " has :owner-authority")))))
