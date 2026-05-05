## 1. 文書の目的

この文書は、バックテスト本体の仕様を定義するものです。

バックテストでは、過去K線CSVから読み込んだ List<Kline> を使い、既存の売買戦略を過去データ上で動かします。

これにより、実際の注文を行う前に、売買戦略の動きや成績を確認できるようにします。

## 2. 機能概要

この機能は、過去のK線データを古い順に1本ずつ処理し、その時点のデータを使って売買判定を行います。

処理の概要は以下です。

- 過去K線CSVを読み込み、List<Kline> を取得する
- 使用する売買戦略を選択する
- 初期資金を設定する
- K線を openTime 昇順に1本ずつ処理する
- 各時点で TradingStrategy に売買判定を依頼する
- SimulationService を使って仮想の残高・保有数量・損益を更新する
- 各時点の資産状況を記録する
- 最後にバックテスト結果をまとめる

## 3. 入力仕様

この機能は、以下の値を入力として受け取ります。

| 項目                         | 例                                                          | 説明                               |
| ---------------------------- | ----------------------------------------------------------- | ---------------------------------- |
| BACKTEST_KLINE_CSV_PATH      | data/backtest/input/btc_5min_20260501_20260531.csv          | 読み込む過去K線CSVファイルのパス   |
| BACKTEST_STRATEGY_NAME       | SafeReboundStrategy                                         | 使用する売買戦略名                 |
| BACKTEST_INITIAL_CAPITAL     | 1000000                                                     | バックテスト開始時の仮想資金       |
| BACKTEST_SUMMARY_OUTPUT_PATH | data/backtest/output/summary_btc_5min_20260501_20260531.csv | バックテスト結果(サマリー)の出力先 |
| BACKTEST_STEPS_OUTPUT_PATH   | data/backtest/output/steps_btc_5min_20260501_20260531.csv   | バックテスト結果(明細)の出力先     |
| APP_CONFIG_PATH              | config/application-test.yaml                                | 読み込む設定ファイルのパス         |

入力例:

```
BACKTEST_KLINE_CSV_PATH=data/backtest/input/btc_5min_20260501_20260531.csv
BACKTEST_STRATEGY_NAME=SafeReboundStrategy
BACKTEST_INITIAL_CAPITAL=1000000
BACKTEST_SUMMARY_OUTPUT_PATH=data/backtest/output/summary_btc_5min_20260501_20260531.csv
BACKTEST_STEPS_OUTPUT_PATH=data/backtest/output/steps_btc_5min_20260501_20260531.csv
```

## 4. 出力仕様

バックテスト結果として、サマリー情報と各時点の明細情報を出力します。

サマリー情報は、バックテスト全体の成績を表します。

| 項目                  | 説明                   |
| --------------------- | ---------------------- |
| strategyName          | 使用した売買戦略名     |
| initialCapital        | 開始時点の仮想資金     |
| finalAssetValue       | 終了時点の総資産額     |
| realizedProfitAndLoss | 確定損益               |
| totalReturnRate       | 初期資金に対する増減率 |
| tradeCount            | 売買回数               |
| buyCount              | 買い回数               |
| sellCount             | 売り回数               |
| maxDrawdown           | 最大ドローダウン       |

**各項目の出力形式と数え方:**

- **totalReturnRate**: 小数で出力します（例: 10%の利益なら 0.10、5%の損失なら -0.05）。
- **maxDrawdown**: 小数で出力します（例: 10%下落なら 0.10）。途中の最高資産額からどれだけ下がったかを表し、一時的にどれくらい資産が減ったかを見るための値です。
- **buyCount**: 実際に仮想購入が成立した回数です。
- **sellCount**: 実際に仮想売却が成立した回数です。
- **tradeCount**: buyCount と sellCount の合計（buyCount + sellCount）です。

明細情報は、各K線時点での状態を表します。

| 項目                  | 説明                               |
| --------------------- | ---------------------------------- |
| openTime              | K線の開始時刻                      |
| close                 | そのK線の終値                      |
| action                | 売買判定結果                       |
| reason                | 判定理由                           |
| cashBalance           | 仮想の現金残高                     |
| holdingAmount         | 仮想の保有数量                     |
| buyPrice              | 保有中の買値                       |
| realizedProfitAndLoss | その時点までの確定損益             |
| estimatedHoldingValue | 保有数量を現在価格で評価した金額   |
| totalAssetValue       | 現金残高と評価額を合計した総資産額 |

## 5. 処理仕様

処理は以下の順番で行います。

1. 入力値から過去K線CSVのパスを取得する
2. 過去K線CSV読み込み機能を使って List<Kline> を取得する
3. 使用する売買戦略を取得する
4. 初期資金から SimulationState を作成する
5. K線データを openTime 昇順に処理する
6. 各時点で、その時点までのK線一覧を TradingStrategy に渡す
7. TradingStrategy の判定結果を受け取る
8. K線の close を現在価格として扱う
9. SimulationService を使って状態を更新する
10. cashBalance、holdingAmount、buyPrice、realizedProfitAndLoss を記録する
11. estimatedHoldingValue と totalAssetValue を計算する
12. 全K線の処理が完了したらサマリー情報を作成する
13. バックテスト結果をサマリー用と明細用の2つのファイルに出力する

## 6. 売買判定の扱い

売買判定は、既存の TradingStrategy を使います。

バックテスト本体は、売買条件そのものを持ちません。
買うか、売るか、見送るかの判断は、選択された Strategy に任せます。

使用する戦略の例は以下です。

| 戦略名                   | 説明                                     |
| ------------------------ | ---------------------------------------- |
| SafeReboundStrategy      | 買値を考慮して利確・損切りを判断する戦略 |
| SimpleContrarianStrategy | 比較用に残している旧ロジック             |

バックテスト本体は、Strategy から返された判定結果をもとに、SimulationService で仮想状態を更新します。

## 7. 資産計算の扱い

各時点の資産は、以下の考え方で計算します。

| 項目                  | 計算方法                                            |
| --------------------- | --------------------------------------------------- |
| estimatedHoldingValue | holdingAmount × 現在価格                            |
| totalAssetValue       | cashBalance + estimatedHoldingValue                 |
| finalAssetValue       | 最後のK線時点の totalAssetValue                     |
| totalReturnRate       | (finalAssetValue - initialCapital) / initialCapital |
| maxDrawdown           | 過去最高の totalAssetValue からの下落率の最大値     |

現在価格には、その時点の Kline.close を使います。

価格や数量の計算には BigDecimal を使います。
BigDecimal は、小数を正確に扱うための型です。

## 8. K線データの扱い

バックテストでは、読み込んだ K線データを openTime 昇順で処理します。

同じ openTime のK線が複数ある場合は、過去K線CSV読み込み機能側で1件にまとめられている前提です。

K線が不足している場合の判定は、Strategy の結果を尊重します。
例えば、Strategy がデータ不足として売買を見送る場合、バックテスト本体はその判定結果をそのまま扱います。

## 9. エラー仕様

以下の場合はエラーとして扱います。

| 条件                                       | 扱い         |
| ------------------------------------------ | ------------ |
| 過去K線CSVファイルのパスが指定されていない | エラーにする |
| 過去K線CSVの読み込みに失敗した             | エラーにする |
| 使用する売買戦略名が指定されていない       | エラーにする |
| 対応していない売買戦略名が指定された       | エラーにする |
| 初期資金が指定されていない                 | エラーにする |
| 初期資金が数値として解釈できない           | エラーにする |
| 初期資金が0以下                            | エラーにする |
| サマリー出力先パスが指定されていない       | エラーにする |
| 明細出力先パスが指定されていない           | エラーにする |
| バックテスト結果の出力に失敗した           | エラーにする |

エラー時は、原因が分かるメッセージにします。

## 10. 実装時の配置方針

実装時は、既存の責務分離に合わせて配置します。

| 層             | 役割                                                                                                        |
| -------------- | ----------------------------------------------------------------------------------------------------------- |
| presentation   | 実行入口                                                                                                    |
| application    | CSV読み込み、Strategy選択、バックテスト実行、結果出力までの流れを管理                                       |
| domain         | バックテストの計算、結果モデル、既存の Kline / SimulationState / TradingStrategy / SimulationService の利用 |
| infrastructure | CSVファイル読み込み、バックテスト結果のファイル出力                                                         |

配置例は以下です。

- domain.backtest.BacktestEngine
- domain.backtest.BacktestResult
- domain.backtest.BacktestSummary
- domain.backtest.BacktestStepResult
- application.BacktestApplication
- presentation.BacktestMain

## 11. 今後の実装順序

この仕様に沿って、次の順番で実装を進めます。

1. バックテスト本体の仕様を追加する
2. バックテストの結果モデルを追加する
3. BacktestEngine を追加する
4. CSV読み込み機能と BacktestEngine をつなぐアプリケーション処理を追加する
5. バックテスト結果を出力する処理を追加する
6. Gradle からバックテストを実行できるようにする
7. Strategyごとの比較結果を出力できるようにする
