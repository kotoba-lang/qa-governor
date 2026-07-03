# kotoba-lang/qa-governor

「Nintendoクオリティ」を項目化した共通QAルーブリック + governor + 監査台帳。
設計は [ADR-2607031100](../../../90-docs/adr/2607031100-kotoba-lang-qa-governor-actor.md)
（superproject `com-junkawasaki/root`）を参照。

CLAUDE.mdの「Actors」節にある既存パターン——知能ノード（LLM）を1ノードに
封じ込め *proposal のみ* 返させ、別系統のgovernorが検閲する。単一不変条件
「governorが拒否するcommitをactorは決して行わない」——をQAに転用したもの。
`ghosthacker-flow`（[ADR-2607023200](../../../90-docs/adr/2607023200-ghosthacker-game-portfolio-flow.md)）
を最初のconsumerとして想定しているが、このrepo自体はドメイン非依存で、
どのkotoba-lang/gftdcojp/etzhayyim/com-junkawasaki配下のrepoからも使える。

## コンセプト

1. **`qa-governor.rubric`** — カテゴリ×重みのルーブリック定義と加重平均
   スコア計算。既定カテゴリ（`stability`/`correctness`/`robustness`/
   `documentation`/`consistency`）はドメイン非依存。consumerは
   `extend-rubric`で固有カテゴリ（例: game-feel）を追加できる。
2. **`qa-governor.governor`** — 知能ノードが返す採点案（`{:category :score
   :evidence}`の集合）を検証する。**LLMの自己申告を無条件で信用しない**——
   evidenceが無い、または高スコアの主張とevidenceの文言が矛盾する場合は
   却下する。
3. **`qa-governor.ledger`** — 承認済みスコアのappend-only監査台帳。
   repo単位の履歴・最新値・推移を追える。

## 現在の実装範囲

pure `.cljc` の3namespaceのみ実装済み（`test/`にテストあり）。
langgraph-clj StateGraphでの実際のQA-LLMノード配線、実リポジトリ
（テスト実行結果・lint結果・git履歴）からのevidence収集ホストアダプタは
未実装——`ghosthacker-flow.core`と同型のレイヤ分離方針（pure core先行、
host adapterは別途）を踏襲している。

## 使い方（イメージ）

```clojure
(require '[qa-governor.rubric :as rubric]
         '[qa-governor.governor :as governor]
         '[qa-governor.ledger :as ledger])

;; 1. 知能ノード(LLM)がproposalを返す(evidence付き)
(def proposal
  [{:category :stability :score 100 :evidence "clean exit in 1.6s, no hang, 3 manual runs"}
   {:category :correctness :score 100 :evidence "109 assertions green, clj-kondo 0 warnings"}])

;; 2. governorが検証する
(def result (governor/evaluate-proposal proposal))
;; => {:approved [...] :rejected [] :all-approved? true}

;; 3. 承認された場合のみ、スコアを計算して台帳に積む(timestamp-msは呼び出し側)
(when (:all-approved? result)
  (let [scores (into {} (map (juxt :category :score) (:approved result)))
        total (rubric/weighted-score rubric/default-rubric scores)]
    (ledger/record ledger/empty-ledger
                    {:repo "ghosthacker-flow"
                     :timestamp-ms 0
                     :scores scores
                     :total total
                     :grade (rubric/grade total)
                     :verdict :approved})))
```

## 開発

```bash
clojure -M:test
```

Lint（clj-kondo、Clojars経由でHomebrew等の別インストール不要）:

```bash
clojure -M:lint
```

## ライセンス

MIT License — [LICENSE](LICENSE) 参照。
