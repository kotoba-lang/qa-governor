(ns qa-governor.collectors.robustness
  "kotoba-lang/qa-governor — deterministic (non-LLM) robustness check
  (ADR-2607031100): does the test suite exercise error paths (`thrown?`
  assertions) and have tests explicitly named around guards/boundaries/
  exploits? Pure text analysis over test source; the host adapter supplies
  the concatenated test-source string.

  Limitation (honest): a naive proxy — counting `thrown?` and keyword
  matches in `deftest` names doesn't verify the tests are meaningful, only
  that the *shape* of edge-case testing exists.")

(defn count-error-path-assertions
  "test-source中の `(is (thrown? ...` の出現数。"
  [test-source]
  (count (re-seq #"\(is\s+\(thrown\?" test-source)))

(def ^:private guard-keyword-pattern
  #"(?i)deftest[^\n]*(guard|mash|boundary|edge|invalid|exploit|once)[^\n]*")

(defn count-guard-tests
  "deftest名にguard/mash/boundary/edge/invalid/exploit/onceのいずれかを
   含むテストの数（対マッシュガードや境界値ガードのような、悪用/異常系を
   狙い撃ちしたテストが存在するかの目印）。"
  [test-source]
  (count (re-seq guard-keyword-pattern test-source)))

(defn robustness-entry
  [{:keys [test-source]}]
  (let [error-paths (count-error-path-assertions test-source)
        guard-tests (count-guard-tests test-source)
        score (min 100 (+ (* 20 (min error-paths 3))
                           (* 20 (min guard-tests 2))))]
    {:category :robustness
     :score score
     :evidence (str error-paths " thrown? assertion(s), " guard-tests
                     " guard/boundary-named deftest(s) found in test source")}))
