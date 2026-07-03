(ns qa-governor.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [qa-governor.governor :as governor]))

(deftest evaluate-entry-test
  (testing "evidence無しは却下"
    (is (= :rejected (:verdict (governor/evaluate-entry
                                 {:category :stability :score 100 :evidence nil})))))
  (testing "evidenceが空文字も却下"
    (is (= :rejected (:verdict (governor/evaluate-entry
                                 {:category :stability :score 100 :evidence ""})))))
  (testing "高スコアなのにevidenceが矛盾していれば却下"
    (is (= :rejected (:verdict (governor/evaluate-entry
                                 {:category :stability :score 100
                                  :evidence "process hung for 15s before the shutdown-agents fix"})))))
  (testing "evidenceが妥当なら承認"
    (is (= :approved (:verdict (governor/evaluate-entry
                                 {:category :stability :score 100
                                  :evidence "exits cleanly in 1.6s across 3 manual runs"})))))
  (testing "矛盾パターンが定義されていないカテゴリは評価対象外(evidenceがあれば承認)"
    (is (= :approved (:verdict (governor/evaluate-entry
                                 {:category :documentation :score 100
                                  :evidence "README up to date"}))))))

(deftest evaluate-proposal-test
  (testing "1件でも却下があればall-approved?はfalse"
    (let [result (governor/evaluate-proposal
                   [{:category :stability :score 100 :evidence "exits cleanly, no issue observed"}
                    {:category :correctness :score 100 :evidence nil}])]
      (is (false? (:all-approved? result)))
      (is (= 1 (count (:approved result))))
      (is (= 1 (count (:rejected result))))))
  (testing "全件evidenceありならall-approved?はtrue"
    (let [result (governor/evaluate-proposal
                   [{:category :stability :score 90 :evidence "consistently exits within 2s across CI runs"}
                    {:category :correctness :score 90 :evidence "109 assertions green"}])]
      (is (true? (:all-approved? result))))))
