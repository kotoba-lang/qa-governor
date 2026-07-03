(ns qa-governor.collectors.static-analysis-shell
  "kotoba-lang/qa-governor — JVM host adapter that reads source files from a
  project directory and hands the text to the pure static-analysis
  collectors (stability/robustness/documentation, ADR-2607031100). This is
  where real file I/O happens; the collectors themselves stay pure and
  host-free."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [qa-governor.collectors.stability :as stability]
            [qa-governor.collectors.robustness :as robustness]
            [qa-governor.collectors.documentation :as documentation]))

(defn- clj-source-files [dir]
  (when (.isDirectory (io/file dir))
    (->> (file-seq (io/file dir))
         (filter #(.isFile %))
         (filter #(re-find #"\.clj[cs]?$" (.getName %))))))

(defn- slurp-all [files]
  (str/join "\n" (map slurp files)))

(defn- main-bearing-sources
  "project-dir/src配下で`defn -main`を含むファイルの中身だけを返す。"
  [project-dir]
  (->> (clj-source-files (io/file project-dir "src"))
       (filter #(str/includes? (slurp %) "defn -main"))
       (mapv slurp)))

(defn collect
  "project-dirを静的解析し、stability/robustness/documentationのproposal
   entryをまとめて返す。elapsed-test-ms(任意)はcorrectness収集側で計測した
   ものを渡すとstabilityスコアに反映される。"
  [project-dir & [elapsed-test-ms]]
  (let [source (slurp-all (clj-source-files (io/file project-dir "src")))
        test-source (slurp-all (clj-source-files (io/file project-dir "test")))
        readme? (.exists (io/file project-dir "README.md"))
        changelog? (.exists (io/file project-dir "CHANGELOG.md"))]
    [(stability/stability-entry {:main-sources (main-bearing-sources project-dir)
                                  :elapsed-test-ms elapsed-test-ms})
     (robustness/robustness-entry {:test-source test-source})
     (documentation/documentation-entry {:source source
                                          :readme-exists? readme?
                                          :changelog-exists? changelog?})]))
