# PR07: 実注文機能を Phase1 の仕様から分離する

| 項目 | 内容 |
| --- | --- |
| 想定読者 | この改善を実施する開発者、AIコーディングエージェント |
| 読んだあとできること | 実注文の仕様を Phase1 の仕様から分離して記述できる |
| 状態 | 実施済み（ブランチ `docs/real-order-phase-separation`） |
| 機密区分 | 公開可 |
| 作成者 | リポジトリ管理者 |
| 保守責任者 | リポジトリ管理者 |
| 最終確認日 | 2026-08-30 |


## 対象の指摘

[findings.md](findings.md) の **E**（重要度: 高）

## なぜ直すか

仕様とコードが正面から矛盾しています。

- [phase1-simulation.md](../specifications/phase1-simulation.md) の「対象外」に2つが挙がっている。「実資金を使った実際の注文」と「GMOコイン Private API の利用」である。
- [roadmap.md](../overview/roadmap.md) の Phase1 禁止事項に「**実際の注文を送ること**」と書かれている。
- しかし [RealTradingService.kt](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/realtrading/RealTradingService.kt) は `placeOrder` まで実装済みである。
[secrets.tf](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/infra/terraform/gcp/secrets.tf) は GMO APIキーの Secret も作っている。

どちらが正なのか読み手が判断できません。**実装は消しません。** 仕様側で位置づけ直します。「Phase3 の先行実装であり Phase1 では実行できない」とします（[PR05](pr05-phase1-real-order-guard.md) で実際に実行できなくします）。

分離先を Phase3 にする理由は次のとおりです。[roadmap.md](../overview/roadmap.md) 上、実注文は Phase3 の内容です。Phase2 の禁止事項にも「自動で実際の注文を出すこと」があります。

## 変更対象

このPRは**ドキュメントのみ**を変更します。コードは変更しません。

| ファイル | 変更内容 |
| --- | --- |
| [docs/specifications/phase1-simulation.md](../specifications/phase1-simulation.md) | 実注文の位置づけを明記する節を追加 |
| [docs/overview/roadmap.md](../overview/roadmap.md) | Phase3 に「先行実装済みの範囲」を追記 |
| [docs/specifications/features/real-trading-gmo-order.md](../specifications/features/real-trading-gmo-order.md) | 冒頭に Phase3 スコープである旨を明記 |
| [docs/README.md](../README.md) | 案内に反映 |
| [README.md](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/README.md) | 「Phase1の制約」節に反映 |

## 実施手順

1. **[phase1-simulation.md](../specifications/phase1-simulation.md)**: 「2. 対象範囲」の「対象外」はそのまま残す。その直後に節を追加する。以下は挿入する本文の案（リンクは挿入先の `docs/specifications/phase1-simulation.md` から見た相対パス）。

   ```markdown
   ### 実注文機能の位置づけ

   リアル注文機能（GMOコイン Private API の利用、`RealTradingService`）は Phase3 のスコープですが、
   コード上には先行実装されています。Phase1 では起動時ガードにより実行できません
   （`app.phase: 1` かつ `real_trade_enabled: true` の場合は異常終了します）。
   仕様は [リアル購入処理（GMOコイン） 仕様書](features/real-trading-gmo-order.md) を参照してください。
   ```

2. **[roadmap.md](../overview/roadmap.md)**: Phase3 の節に「先行実装済みの範囲」を追加する。

   - 実装済み: Private API クライアント、署名生成、安全チェック、買い注文の送信と約定確認
   - 未実装: 通知、手動承認フロー、売り注文の自動化、承認ログ
   - Phase1/Phase2 の禁止事項は維持する。「実装はあるが既定で無効」と書き分ける

3. **[real-trading-gmo-order.md](../specifications/features/real-trading-gmo-order.md)**: 「1. 文書の目的」の前に注記を置く。

   > **この仕様は Phase3（実注文 + 手動承認 + 安全制御）のスコープです。** Phase1 では有効化しません。現在の運用設定は `dry_run: true` / `real_trade_enabled: false` で固定されており、Phase1 では起動時ガードにより実注文経路に入りません。

4. **[docs/README.md](../README.md)**: 「🧠 売買ロジックの仕組みを知りたい方」にリンクを足す。実注文仕様が Phase3 のものである旨を添える。

5. **[README.md](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/README.md)**: 「注意 (Phase1の制約)」に1行追加する。実注文機能は先行実装されているが Phase1 では実行できない、と書く。

## 受け入れ条件

- [ ] Phase1 仕様書だけで「実注文コードはあるが動かない」と分かること
- [ ] ロードマップの禁止事項が、実装の存在と矛盾しない書き方であること
- [ ] `real-trading-gmo-order.md` が Phase3 の仕様であると冒頭で分かること
- [ ] コードに変更が無いこと
- [ ] リポジトリ内文書への参照が Markdown リンクになっていること

## 検証

ドキュメントのみのため `./gradlew build` は不要。次を目視で確認します。

- 追加・変更したリンクがすべて有効であること
- [docs/README.md](../README.md) の「ドキュメントリンク方針」に沿っていること

## スコープ外

- 実注文を実行不能にする実装（[PR05](pr05-phase1-real-order-guard.md)）
- その他の仕様書の食い違い（[PR08](pr08-doc-consistency.md)）
