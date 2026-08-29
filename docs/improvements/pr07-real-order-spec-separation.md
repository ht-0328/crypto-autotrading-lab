# PR07: 実注文機能を Phase1 の仕様から分離する

**状態**: 実施済み（ブランチ `docs/real-order-phase-separation`）

## 対象の指摘

[findings.md](findings.md) の **E**（重要度: 高）

## なぜ直すか

仕様とコードが正面から矛盾しています。

- [phase1-simulation.md](../specifications/phase1-simulation.md) の「対象外」に「実資金を使った実際の注文（リアル注文）」「GMOコイン Private API の利用」と書かれている。
- [roadmap.md](../overview/roadmap.md) の Phase1 禁止事項に「**実際の注文を送ること**」と書かれている。
- しかし [RealTradingService.kt](../../projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/realtrading/RealTradingService.kt) は `placeOrder` まで実装済みで、[secrets.tf](../../infra/terraform/gcp/secrets.tf) は GMO APIキーの Secret も作っている。

どちらが正なのか読み手が判断できません。**実装を消すのではなく、仕様側で「Phase3 の先行実装であり Phase1 では実行できない」と位置づけ直します**（[pr05-phase1-real-order-guard.md](pr05-phase1-real-order-guard.md) で実際に実行できなくします）。

分離先を Phase3 にする理由: [roadmap.md](../overview/roadmap.md) 上、実注文は Phase3「実注文 + 手動承認 + 安全制御」の内容です。Phase2 の禁止事項にも「自動で実際の注文を出すこと」があります。

## 変更対象

このPRは**ドキュメントのみ**を変更します。コードは変更しません。

| ファイル | 変更内容 |
| --- | --- |
| [docs/specifications/phase1-simulation.md](../specifications/phase1-simulation.md) | 実注文の位置づけを明記する節を追加 |
| [docs/overview/roadmap.md](../overview/roadmap.md) | Phase3 に「先行実装済みの範囲」を追記 |
| [docs/specifications/features/real-trading-gmo-order.md](../specifications/features/real-trading-gmo-order.md) | 冒頭に Phase3 スコープである旨を明記 |
| [docs/README.md](../README.md) | 案内に反映 |
| [README.md](../../README.md) | 「Phase1の制約」節に反映 |

## 実施手順

1. **[phase1-simulation.md](../specifications/phase1-simulation.md)**: 「2. 対象範囲」の「対象外」はそのまま残し、その直後に節を追加する。以下は挿入する本文の案（リンクは挿入先の `docs/specifications/phase1-simulation.md` から見た相対パス）。

   ```markdown
   ### 実注文機能の位置づけ

   リアル注文機能（GMOコイン Private API の利用、`RealTradingService`）は Phase3 のスコープですが、
   コード上には先行実装されています。Phase1 では起動時ガードにより実行できません
   （`app.phase: 1` かつ `real_trade_enabled: true` の場合は異常終了します）。
   仕様は [リアル購入処理（GMOコイン） 仕様書](features/real-trading-gmo-order.md) を参照してください。
   ```

2. **[roadmap.md](../overview/roadmap.md)**: Phase3 の節に「先行実装済みの範囲」を追加する。

   - 実装済み: Private API クライアント、署名生成、安全チェック（`RealTradingSafetyChecker`）、買い注文の送信と約定確認
   - 未実装: 通知、手動承認フロー、売り注文の自動化、承認ログ
   - Phase1/Phase2 の禁止事項は維持し、「実装はあるが既定で無効かつ起動時ガードで実行不能」と書き分ける

3. **[real-trading-gmo-order.md](../specifications/features/real-trading-gmo-order.md)**: 「1. 文書の目的」の前に注記を置く。

   > **この仕様は Phase3（実注文 + 手動承認 + 安全制御）のスコープです。** Phase1 では有効化しません。現在の運用設定は `dry_run: true` / `real_trade_enabled: false` で固定されており、Phase1 では起動時ガードにより実注文経路に入りません。

4. **[docs/README.md](../README.md)**: 「🧠 売買ロジックの仕組みを知りたい方」または新しい項目に、実注文仕様が Phase3 のものである旨を添えてリンクする。

5. **[README.md](../../README.md)**: 「本プロジェクトの前提事項と注意事項」の「注意 (Phase1の制約)」に、実注文機能が先行実装されているが Phase1 では実行できないことを1行追加する。

## 受け入れ条件

- [ ] Phase1 仕様書を読んだだけで「実注文コードはあるが Phase1 では動かない」と分かること
- [ ] ロードマップの Phase1/Phase2 禁止事項が、実装の存在と矛盾しない書き方になっていること
- [ ] `real-trading-gmo-order.md` が Phase3 の仕様であると冒頭で分かること
- [ ] コードに変更が無いこと
- [ ] リポジトリ内文書への参照がすべて相対パスの Markdown リンクになっていること（[docs/README.md](../README.md) のドキュメントリンク方針）

## 検証

ドキュメントのみのため `./gradlew build` は不要。次を目視で確認する。

- 追加・変更したリンクがすべて有効であること
- [docs/README.md](../README.md) の「ドキュメントリンク方針」に沿っていること（相対パス、ラベル付きリンク、ファイル名の直書き禁止）

## スコープ外

- 実注文を実行不能にする実装（[pr05-phase1-real-order-guard.md](pr05-phase1-real-order-guard.md)）
- その他の仕様書の食い違い（[pr08-doc-consistency.md](pr08-doc-consistency.md)）
