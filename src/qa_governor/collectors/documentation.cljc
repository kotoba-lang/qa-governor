(ns qa-governor.collectors.documentation
  "kotoba-lang/qa-governor — deterministic (non-LLM) documentation-coverage
  check (ADR-2607031100): what fraction of public `defn`s have a docstring,
  plus whether README.md/CHANGELOG.md exist. Pure text analysis only —
  reading files from disk is the host adapter's job
  (`qa-governor.collectors.static-analysis-shell`).

  Limitation (honest): a regex-based scan, not a real reader/AST parse. It
  will miss docstrings that contain escaped quotes, and won't understand
  reader macros. Good enough as a coverage signal, not a certifier.")

(defn parse-defn-docstring-coverage
  "source(複数ファイル分を連結した文字列でも良い)中の`(defn name ...)`
   （`defn-`は除く=公開APIのみ対象）を数え、直後にdocstring文字列がある
   ものの割合を返す。defnが1つも無ければ100%扱い（『無いから減点』の
   誤検知を避ける——documentation-entry側でREADME/CHANGELOGの有無は別途見る）。"
  [source]
  (let [defn-matches (re-seq #"\(defn\s+([a-zA-Z][\w?!*+<>=-]*)\s*(\"[^\"]*\")?" source)
        total (count defn-matches)
        documented (count (filter (fn [[_ _ doc]] (some? doc)) defn-matches))]
    {:total total
     :documented documented
     :ratio (if (zero? total) 1.0 (/ (double documented) total))}))

(defn documentation-entry
  "source: 全.clj/.cljc/.cljsソースを連結した文字列。
   readme-exists?/changelog-exists?: プロジェクト直下に存在するか。"
  [{:keys [source readme-exists? changelog-exists?]}]
  (let [{:keys [total documented ratio]} (parse-defn-docstring-coverage source)
        doc-score (* 100 ratio)
        readme-penalty (if readme-exists? 0 20)
        changelog-penalty (if changelog-exists? 0 10)
        score (max 0 (min 100 (- doc-score readme-penalty changelog-penalty)))]
    {:category :documentation
     :score (long score)
     :evidence (str "public defns documented: " documented "/" total
                     " (" (int (* 100 ratio)) "%)"
                     (when-not readme-exists? ", README.md missing")
                     (when-not changelog-exists? ", CHANGELOG.md missing"))}))
