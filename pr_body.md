## 概要

`TrendConfirmReboundStrategy` の仕様書に基づき、同戦略を実装しました。この戦略は `CooldownReboundStrategy` のロジックをベースに、短期トレンド（MA5）の上抜け確認を買い条件に追加したものです。

## 変更種類（必須）

- [x] feature
- [ ] fix
- [ ] refactor
- [ ] docs
- [ ] chore

## スコープ宣言（必須）

- 対象: 新規売買戦略 `TrendConfirmReboundStrategy` の追加および設定連携、テスト実装
- 非対象: 既存戦略のロジック変更、運用フローの変更

## 変更内容

- `cryptoautotrading.domain.strategy.TrendConfirmReboundStrategy` を新規作成
  - 買い条件にMA5（直近5本の終値平均）の上抜け確認ロジックを追加
- `cryptoautotrading.application.TradingApplication` および `BacktestApplication` の `createStrategy` に新規戦略を追加
- `TrendConfirmReboundStrategyTest` を新規作成し、単体テストを実装
- `TradingApplicationTest` および `BacktestApplicationTest` に新規戦略用のテストを追加

## 影響範囲

- 新たに `TrendConfirmReboundStrategy` を設定（環境変数またはyaml）で指定可能になります。
- 既存の戦略（`SafeReboundStrategy`, `CooldownReboundStrategy` など）の挙動には影響しません。

## 確認手順

1. `projects/crypto-autotrading-app` ディレクトリにて `./gradlew test` を実行し、すべてのテストがパスすることを確認。
2. アプリケーション実行時に `strategy_name` に `TrendConfirmReboundStrategy` を指定し、正常にインスタンス化されることを確認（テストにて検証済み）。

## 分割方針チェック

- [x] リファクタと機能追加を同一PRに混在させていない ([docs/ai/change-granularity.md](../docs/ai/change-granularity.md) 参照)
- [x] `domain` 変更時に `infrastructure` を同時変更していない（必要時は別PR） ([docs/ai/kotlin-boundary-rules.md](../docs/ai/kotlin-boundary-rules.md) 参照)
- [x] `application` にオーケストレーション以外のロジックを追加していない
