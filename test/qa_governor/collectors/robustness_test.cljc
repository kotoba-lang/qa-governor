(ns qa-governor.collectors.robustness-test
  (:require [clojure.test :refer [deftest is testing]]
            [qa-governor.collectors.robustness :as robustness]))

(def ^:private rich-test-source
  "(deftest beat-interval-ms-guard-test\n  (is (thrown? AssertionError (core/beat-interval-ms 0)))\n  (is (thrown? AssertionError (core/beat-interval-ms -10))))\n(deftest judge-input-once-test\n  (is (= :perfect :perfect)))\n")

(def ^:private thin-test-source
  "(deftest basic-test\n  (is (= 1 1)))\n")

(deftest count-error-path-assertions-test
  (is (= 2 (robustness/count-error-path-assertions rich-test-source)))
  (is (= 0 (robustness/count-error-path-assertions thin-test-source))))

(deftest count-guard-tests-test
  (is (= 2 (robustness/count-guard-tests rich-test-source)))
  (is (= 0 (robustness/count-guard-tests thin-test-source))))

(deftest robustness-entry-test
  (testing "thrown?とguard/once named testが両方あれば高スコア"
    (let [entry (robustness/robustness-entry {:test-source rich-test-source})]
      (is (= :robustness (:category entry)))
      (is (pos? (:score entry)))))
  (testing "何も無ければ0点"
    (is (= 0 (:score (robustness/robustness-entry {:test-source thin-test-source}))))))
