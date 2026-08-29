# 改善計画 (Improvements)

| 項目 | 内容 |
| --- | --- |
| 想定読者 | 改善作業に着手する開発者、AIコーディングエージェント |
| 読んだあとできること | どの改善から着手すべきかを選び、その1件だけを実施できる |
| 状態 | 現行 |
| 機密区分 | 公開可 |
| 作成者 | リポジトリ管理者 |
| 保守責任者 | リポジトリ管理者 |
| 最終確認日 | 2026-08-30 |


## 文書の目的

- 仕様・ドキュメント・インフラ・実装の食い違いを洗い出した結果と、その解消計画を管理する
- 1件ずつ独立して着手できるように、作業単位ごとにファイルを分ける

## 対象読者

開発メンバー、AIコーディングエージェント

## 関連ドキュメント

- [指摘一覧と根拠](findings.md)
- [第3波バックログ](backlog.md)
- [PRとコミットのルール](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/.agents/skills/pr-and-commit/SKILL.md)

## 使い方

作業を依頼するときは、実施したいファイルのパスを指定してください。各ファイルは単体で完結しており、対象の指摘・変更対象・実施手順・受け入れ条件・検証手順が書かれています。

```text
docs/improvements/pr03-private-api-log-leak.md の内容を実施して
```

- 1ファイル = 1PR です。複数を1つのPRにまとめないでください（[pr-and-commit](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/.agents/skills/pr-and-commit/SKILL.md)）。
- 着手したら、下の一覧の「状態」を更新してください。

## この計画の背景

Claude Code / Codex / Antigravity の3ツールで同じリポジトリをレビューし、結果を突き合わせて作成しました（2026-08-29）。

最大の問題は Kotlin コードの品質ではなく、**仕様書・ドキュメント・インフラコード・実装の4者が互いに食い違っていること**でした。特に次の3点が重い問題です。

- Cloud Run では設定ファイルが存在せず、`API_PUBLIC_BASE_URL` などの環境変数も名前違いで無視される。
- [docs/overview/product.md](../overview/product.md) が「安全設計」として宣言している内容（時刻ズレ検知・指数バックオフ・重複実行スキップ）が実装されていない。
- バックテストが同一足の終値で判定し同じ終値で約定し、手数料・スリッページも考慮していない（実測の結果、影響は当初の想定より小さかった。[findings.md](findings.md) の「誤りだった指摘」を参照）。

個々の指摘とその根拠は [findings.md](findings.md) を参照してください。

## 決定事項

1. **実注文機能は Phase1 の仕様から分離する**。分離先は **Phase3**（[roadmap.md](../overview/roadmap.md) 上、実注文は「通知 → 手動承認 → 実注文」の Phase3 の内容）。
2. **gcloud / Terraform の二重管理は、今回は乖離の解消までにとどめる**。一本化は [backlog.md](backlog.md) 送り。

## 第1波: 実害を止める

| 状態 | ファイル | 内容 | 重要度 |
| --- | --- | --- | --- |
| 実施済み | [pr01-docker-clean-scope.md](pr01-docker-clean-scope.md) | ホストの Docker を全消しするスクリプトを安全にする | 高 |
| 実施済み | [pr03-private-api-log-leak.md](pr03-private-api-log-leak.md) | Private API レスポンスのログ流出を止める | 高 |
| 実施済み | [pr02-cloud-run-config.md](pr02-cloud-run-config.md) | Cloud Run で設定が効かない・ログが残らない問題を直す | 高 |
| 実施済み | [pr04-state-repository-crash-safe.md](pr04-state-repository-crash-safe.md) | 状態保存をクラッシュセーフにする | 高 |
| 実施済み | [pr05-phase1-real-order-guard.md](pr05-phase1-real-order-guard.md) | Phase1 で実注文を構造的に不可能にする | 高 |

## 第2波: 仕様・ドキュメント・CI の整合

| 状態 | ファイル | 内容 | 重要度 |
| --- | --- | --- | --- |
| 実施済み | [pr09-ci-compose-consistency.md](pr09-ci-compose-consistency.md) | CI / Compose の整合と安全側固定 | 高 |
| 実施済み | [pr06-backtest-execution-model.md](pr06-backtest-execution-model.md) | バックテストの約定モデルを是正する | 高 |
| 実施済み | [pr07-real-order-spec-separation.md](pr07-real-order-spec-separation.md) | 実注文機能を Phase1 の仕様から分離する | 高 |
| 実施済み | [pr08-doc-consistency.md](pr08-doc-consistency.md) | 仕様書・設計書の食い違いを解消する | 中 |
| 実施済み | [pr10-config-fail-fast.md](pr10-config-fail-fast.md) | 設定の fail-fast と環境変数契約の統一 | 中 |

## 第4波: ドキュメントサイト（Zensical）への対応

`docs/` 配下を [Zensical](https://zensical.org/) で静的サイト化し、[GitHub Pages](https://ht-0328.github.io/crypto-autotrading-lab/) で公開しています。これにより、これまで問題にならなかった書き方が公開サイト上で実害を出しています。

| 状態 | ファイル | 内容 | 重要度 |
| --- | --- | --- | --- |
| 実施済み | [pr11-zensical-broken-links.md](pr11-zensical-broken-links.md) | 公開サイトのリンク切れ197件を解消し、再発をCIで止める | 高 |
| 実施済み | [pr12-zensical-readability.md](pr12-zensical-readability.md) | 安全上の警告を admonition にし、章タイトルと目次の食い違いを直す | 中 |

**[pr11-zensical-broken-links.md](pr11-zensical-broken-links.md) を先に実施してください。** 両方とも同じファイルを触るため、順序を逆にすると衝突します。

## 推奨する着手順

**第1波: 実害を止める**

1. [pr01-docker-clean-scope.md](pr01-docker-clean-scope.md)（実行1回で被害が出るため最優先）
2. [pr03-private-api-log-leak.md](pr03-private-api-log-leak.md) → [pr02-cloud-run-config.md](pr02-cloud-run-config.md)（ログ流出を止めてから標準出力へ流す。順序が逆だと流出が増える）
3. [pr04-state-repository-crash-safe.md](pr04-state-repository-crash-safe.md)
4. [pr05-phase1-real-order-guard.md](pr05-phase1-real-order-guard.md)

**第2波: 整合を取る**

5. [pr09-ci-compose-consistency.md](pr09-ci-compose-consistency.md)
6. [pr06-backtest-execution-model.md](pr06-backtest-execution-model.md)
7. [pr07-real-order-spec-separation.md](pr07-real-order-spec-separation.md) → [pr08-doc-consistency.md](pr08-doc-consistency.md) → [pr10-config-fail-fast.md](pr10-config-fail-fast.md)

**第4波: ドキュメントサイト**

8. [pr11-zensical-broken-links.md](pr11-zensical-broken-links.md) → [pr12-zensical-readability.md](pr12-zensical-readability.md)（どちらも実装には影響しないため、第1波・第2波とは独立に進められる）

## 今回やらないこと

3ツール共通の判断として、次は着手しません。

- **実注文機能（SELL 自動化・Private API 拡張）の作り込み**: Phase1 では起動時ガードで封じるのが先。誤発注リスクを増やすだけ。
- **`ALL_IN` の高度化（動的ポジションサイジング）**: Phase1 の目的は損益シミュレーションの検証。`FIXED_AMOUNT` 中心で足りる。
- **新しい Strategy の追加・パラメータ最適化**: [pr06-backtest-execution-model.md](pr06-backtest-execution-model.md) で約定モデルを直すまで比較結果が信頼できない。
- **detekt / ktlint の導入**: [AGENTS.md](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/AGENTS.md) が「導入されていない」と明記しており今回のスコープ外。
- **Kafka / 分散トランザクション / マイクロサービス化**: Phase1 には過剰。アトミックなファイル保存で足りる。
