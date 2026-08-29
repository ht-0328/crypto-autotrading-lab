# PR06: バックテストの約定モデルを是正する

**状態**: 未着手

## 対象の指摘

[findings.md](findings.md) の **P**（重要度: 高）

## なぜ直すか

[BacktestEngine.kt](../../projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/backtest/BacktestEngine.kt) は、対象の K線を履歴に追加してから判定し、**同じ K線の終値でそのまま約定**させています。

```kotlin
processedKlines.add(kline)
val currentPrice = BigDecimal(kline.close)
val decision = strategy.judge(processedKlines, currentState)
currentState = simulationService.updateState(currentPrice = currentPrice, ...)
```

つまり「終値を見てから、その終値で買える／売れる」前提です。現実には成立しません。手数料・スプレッド・スリッページも一切考慮していません。結果として**全戦略の成績が構造的に楽観化**しており、Phase1 の目的である「戦略の検証」そのものが成り立っていません。

仕様書 [backtest.md](../specifications/features/backtest.md) の処理仕様（6〜8）も同じ前提で書かれているため、**仕様書を直してから実装を直します**。

## 変更対象

| ファイル | 変更内容 |
| --- | --- |
| [docs/specifications/features/backtest.md](../specifications/features/backtest.md) | 約定モデルを「N+1 の始値で約定」に変更。手数料・スリッページを仕様に追加 |
| [docs/architecture/backtest-design.md](../architecture/backtest-design.md) | 設計側の記述を新仕様に合わせる |
| [BacktestEngine.kt](../../projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/backtest/BacktestEngine.kt) | 約定価格を次足の始値にする。手数料・スリッページを反映 |
| [BacktestSummary.kt](../../projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/backtest/BacktestSummary.kt) / [BacktestCsvFileRepository.kt](../../projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/infrastructure/output/BacktestCsvFileRepository.kt) | 前提値（手数料率・スリッページ）を結果に残す |
| [BacktestEngineTest.kt](../../projects/crypto-autotrading-app/src/test/kotlin/cryptoautotrading/domain/backtest/BacktestEngineTest.kt) | 期待値を新モデルに更新 |
| [README.md](../../README.md) | モデル変更前後の結果を比較しない旨を明記 |

## 実施手順

### 1. 仕様書を直す

[backtest.md](../specifications/features/backtest.md) の「7. 処理仕様」を次の内容に置き換える。

- K線 N までの確定データで判定する
- 判定結果は **K線 N+1 の始値**で約定させる
- 最後の K線で出た判定は、約定させる足が無いため実行しない
- 約定価格に手数料率とスリッページを反映する
- 結果 CSV に、使用した手数料率・スリッページを記録する

「5. 入力仕様」に設定項目を追加する。

| 項目 | 例 | 必須 | 説明 |
| --- | --- | --- | --- |
| `BACKTEST_FEE_RATE` | `0.0005` | 任意 | 約定額に対する手数料率。未指定時は 0 |
| `BACKTEST_SLIPPAGE_RATE` | `0.0005` | 任意 | 約定価格に上乗せ／差し引くスリッページ率。未指定時は 0 |

買いは `始値 × (1 + スリッページ率)`、売りは `始値 × (1 - スリッページ率)` とする。

### 2. 実装を直す

`BacktestEngine` のループを、判定と約定を1足ずらす形にする。

- ループ内で判定した `decision` を保持し、次のイテレーションの `kline.open` で `updateState` を呼ぶ。
- 評価額・総資産の記録は従来どおり各足の終値で行う（成績の推移が見えるようにするため）。
- 手数料は約定額に対して差し引き、`realizedProfitAndLoss` に反映する。

### 3. テストを直す

[BacktestEngineTest.kt](../../projects/crypto-autotrading-app/src/test/kotlin/cryptoautotrading/domain/backtest/BacktestEngineTest.kt) の期待値は現行モデル前提なので、新モデルの期待値に更新する。手数料率・スリッページが 0 のケースと、0 でないケースの両方を用意する。

## 受け入れ条件

- [ ] 判定に使った K線の終値では約定しないこと（次足の始値で約定すること）
- [ ] 最後の K線で出た判定が約定しないこと
- [ ] 手数料率・スリッページが 0 のとき、従来より成績が悪化する方向に変わること（look-ahead が消えた証跡）
- [ ] 手数料率・スリッページを設定でき、結果 CSV に前提値が残ること
- [ ] 仕様書・設計書と実装が一致していること

## 検証

```bash
cd projects/crypto-autotrading-app
./gradlew build
```

同一の K線 CSV で、変更前後のバックテストを比較する。

```bash
export BACKTEST_KLINE_CSV_PATH=/workspace/data/local-devcontainer/klines.csv
export BACKTEST_STRATEGY_NAME=SafeReboundStrategy
export BACKTEST_INITIAL_CAPITAL=10000
./gradlew runBacktest
```

確定損益が変更前より悪化する（または取引回数が減る）ことを確認する。改善する場合は実装を疑うこと。

## 注意

- **過去のバックテスト結果は比較不能になります。** README に明記し、過去の成績をもとにした戦略選択はやり直してください。
- ライブ実行（[TradingApplication](../../projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/application/TradingApplication.kt)）は今回変更しません。ライブでは「観測した価格で発注する」動作が現実に近く、look-ahead にはあたらないためです。

## スコープ外

- 新しい Strategy の追加やパラメータ最適化。約定モデルを直すまで比較結果が信頼できないため
- ライブ実行側の約定モデル変更
