## 概要

docs/architecture/backtest.md の「今後の実装順序」に記載されている第2段階「バックテストの結果モデルを追加する」を実施し、バックテスト実行後の結果を保持するためのモデルを追加しました。

## 変更種類（必須）

- [x] feature
- [ ] fix
- [ ] refactor
- [ ] docs
- [ ] chore

## スコープ宣言（必須）

- 対象: バックテストのサマリー情報と各ステップごとの明細情報を保持するデータモデルとテストの追加
- 非対象: 実際のバックテスト実行ロジックやファイル出力処理の追加

## 変更内容

- `cryptoautotrading.domain.backtest.BacktestSummary` の追加 (バックテスト全体のサマリー情報)
- `cryptoautotrading.domain.backtest.BacktestStepResult` の追加 (K線ごとの明細情報)
- `cryptoautotrading.domain.backtest.BacktestResult` の追加 (上記2つを保持するモデル)
- 上記モデルが正しくインスタンス化されることを検証するユニットテスト `BacktestResultTest` の追加

## 影響範囲

- `projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/backtest/`
- `projects/crypto-autotrading-app/src/test/kotlin/cryptoautotrading/domain/backtest/`
(新規追加のみであり、既存ロジックへの影響はありません)

## 確認手順

1. `projects/crypto-autotrading-app/` ディレクトリで `./gradlew test` を実行し、新規追加したテストが成功することを確認する。
2. `projects/crypto-autotrading-app/` ディレクトリで `./gradlew build` を実行し、ビルドが成功することを確認する。

## 分割方針チェック

- [x] リファクタと機能追加を同一PRに混在させていない ([docs/ai/change-granularity.md](../docs/ai/change-granularity.md) 参照)
- [x] `domain` 変更時に `infrastructure` を同時変更していない（必要時は別PR） ([docs/ai/kotlin-boundary-rules.md](../docs/ai/kotlin-boundary-rules.md) 参照)
- [x] `application` にオーケストレーション以外のロジックを追加していない
