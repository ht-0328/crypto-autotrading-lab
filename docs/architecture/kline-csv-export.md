## 1. 文書の目的

この文書は、バックテストで使用する過去K線CSVを作成する機能の仕様を定義するものです。

バックテストでは、過去の価格データを使って売買戦略の動きを検証します。
その入力データとして、GMOコイン Public API から取得したK線データをCSV形式で保存できるようにします。

## 2. 機能概要

この機能は、指定された期間のK線データをGMOコイン Public APIから取得し、バックテストで利用しやすいCSVファイルとして保存します。

処理の概要は以下です。

- 指定された開始日から終了日までの日付範囲を受け取る
- 1日ごとにGMOコイン Public APIのK線取得APIを呼び出す
- 取得したK線データを1つの一覧にまとめる
- openTime の昇順に並べる
- 同じ openTime のデータが重複した場合は1件にまとめる
- バックテスト入力用のCSVファイルとして保存する

## 3. 入力仕様

この機能は、以下の値を入力として受け取ります。

| 項目                     | 例                                            | 説明                               |
| ------------------------ | --------------------------------------------- | ---------------------------------- |
| APP_CONFIG_PATH          | config/application-gmo.yaml                   | アプリケーション設定ファイルのパス |
| APP_DATA_DIR             | ../../data                                    | 出力ファイルの基準ディレクトリ     |
| KLINE_EXPORT_SYMBOL      | BTC                                           | 取得対象の通貨                     |
| KLINE_EXPORT_INTERVAL    | 5min                                          | K線の時間間隔                      |
| KLINE_EXPORT_START_DATE  | 20260501                                      | 取得開始日                         |
| KLINE_EXPORT_END_DATE    | 20260531                                      | 取得終了日                         |
| KLINE_EXPORT_OUTPUT_PATH | backtest/input/btc_5min_20260501_20260531.csv | CSVの出力先パス                    |

日付は yyyyMMdd 形式で指定します。

入力例:

```
APP_DATA_DIR=../../data
KLINE_EXPORT_SYMBOL=BTC
KLINE_EXPORT_INTERVAL=5min
KLINE_EXPORT_START_DATE=20260501
KLINE_EXPORT_END_DATE=20260531
KLINE_EXPORT_OUTPUT_PATH=backtest/input/btc_5min_20260501_20260531.csv
```

## 4. 出力仕様

CSVファイルは、バックテストでそのまま読み込める形式で出力します。

出力先の例:

```
data/backtest/input/btc_5min_20260501_20260531.csv
```

CSVの列は以下とします。

| 列名     | 説明          |
| -------- | ------------- |
| openTime | K線の開始時刻 |
| open     | 始値          |
| high     | 高値          |
| low      | 安値          |
| close    | 終値          |
| volume   | 出来高        |

CSVヘッダー:

```
openTime,open,high,low,close,volume
```

## 5. 処理仕様

処理は以下の順番で行います。

1. アプリケーション設定を読み込む
2. 入力値から取得条件を組み立てる
3. 開始日から終了日までの日付一覧を作成する
4. 各日付についてGMOコイン Public APIのK線取得APIを呼び出す
5. 取得したK線データを一覧に追加する
6. 全日付の取得が完了したら、openTime の昇順に並べる
7. openTime が重複するデータを1件にまとめる
8. CSVファイルとして保存する

## 6. 日付範囲の扱い

KLINE_EXPORT_START_DATE と KLINE_EXPORT_END_DATE は、どちらも取得対象に含めます。

例:

```
KLINE_EXPORT_START_DATE=20260501
KLINE_EXPORT_END_DATE=20260503
```

この場合、以下の3日分を取得します。

- 2026年5月1日
- 2026年5月2日
- 2026年5月3日

## 7. 並び順と重複の扱い

CSVに出力するK線データは、openTime の昇順に並べます。

同じ openTime のデータが複数存在する場合は、1件だけを出力します。
これにより、同じ時刻のK線が重複してバックテストに使われることを防ぎます。

## 8. エラー仕様

以下の場合はエラーとして扱います。

| 条件                                     | 扱い         |
| ---------------------------------------- | ------------ |
| 必須入力値が不足している                 | エラーにする |
| 日付形式が yyyyMMdd ではない             | エラーにする |
| 開始日が終了日より後になっている         | エラーにする |
| GMOコイン Public APIからの取得に失敗した | エラーにする |
| CSVファイルの保存に失敗した              | エラーにする |

取得結果が0件の日付がある場合でも、それだけではエラーにしません。
指定期間全体の取得結果をまとめてCSVに保存します。

## 9. 保存先の扱い

KLINE_EXPORT_OUTPUT_PATH が相対パスの場合は、APP_DATA_DIR 配下のパスとして扱います。

例:

```
APP_DATA_DIR=../../data
KLINE_EXPORT_OUTPUT_PATH=backtest/input/btc_5min_20260501_20260531.csv
```

この場合の出力先は以下です。

```
../../data/backtest/input/btc_5min_20260501_20260531.csv
```

出力先の親ディレクトリが存在しない場合は、CSV保存時に作成します。

## 10. 実装時の配置方針

実装時は、既存の責務分離に合わせて配置します。

| 層             | 役割                                             |
| -------------- | ------------------------------------------------ |
| presentation   | 実行入口                                         |
| application    | 日付範囲の処理、K線取得、CSV保存までの流れを管理 |
| infrastructure | GMO Public API通信、CSVファイル保存              |
| domain         | 既存の Kline モデルを利用                        |

既存の Kline モデルは、CSVの列と対応します。

- openTime
- open
- high
- low
- close
- volume

## 11. 今後の実装順序

この仕様に沿って、次の順番で実装を進めます。

1. 過去K線CSV作成機能の仕様を追加する
2. GMOコイン Public APIから過去K線CSVを作成する機能を実装する
3. 作成したCSVを読み込む機能を実装する
4. 読み込んだK線を使ってバックテスト本体を実装する
5. Strategyごとの比較結果を出力できるようにする
