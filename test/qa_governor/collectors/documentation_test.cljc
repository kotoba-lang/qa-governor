(ns qa-governor.collectors.documentation-test
  (:require [clojure.test :refer [deftest is testing]]
            [qa-governor.collectors.documentation :as documentation]))

(def ^:private source-fully-documented
  "(ns foo)\n(defn bar \"docs\" [x] x)\n(defn- baz [x] x)\n(defn qux \"more docs\" [y] y)\n")

(def ^:private source-partially-documented
  "(ns foo)\n(defn bar \"docs\" [x] x)\n(defn qux [y] y)\n")

(def ^:private source-no-defns
  "(ns foo)\n(def x 1)\n")

(deftest parse-defn-docstring-coverage-test
  (testing "defn-(private)は数えない、公開defnのみ集計する"
    (let [result (documentation/parse-defn-docstring-coverage source-fully-documented)]
      (is (= 2 (:total result)))
      (is (= 2 (:documented result)))
      (is (== 1.0 (:ratio result)))))
  (testing "docstring無しのdefnはdocumentedに数えない"
    (let [result (documentation/parse-defn-docstring-coverage source-partially-documented)]
      (is (= 2 (:total result)))
      (is (= 1 (:documented result)))
      (is (== 0.5 (:ratio result)))))
  (testing "defnが無ければratio=1.0(未doc扱いにしない)"
    (is (== 1.0 (:ratio (documentation/parse-defn-docstring-coverage source-no-defns))))))

(deftest documentation-entry-test
  (testing "全documented + README/CHANGELOGありなら満点"
    (let [entry (documentation/documentation-entry
                  {:source source-fully-documented :readme-exists? true :changelog-exists? true})]
      (is (= :documentation (:category entry)))
      (is (= 100 (:score entry)))))
  (testing "README無しは減点"
    (let [entry (documentation/documentation-entry
                  {:source source-fully-documented :readme-exists? false :changelog-exists? true})]
      (is (< (:score entry) 100))
      (is (re-find #"README.md missing" (:evidence entry)))))
  (testing "CHANGELOG無しも減点"
    (let [entry (documentation/documentation-entry
                  {:source source-fully-documented :readme-exists? true :changelog-exists? false})]
      (is (< (:score entry) 100))
      (is (re-find #"CHANGELOG.md missing" (:evidence entry))))))
