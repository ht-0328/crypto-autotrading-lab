# Phase1 仕様書

## 目的

* 仮想通貨の自動売買アプリを段階的に開発する
* 最終目的は利益を得たい / できるだけ楽したい
* ただし Phase1 は安全優先
* Phase1 は実注文なしのシミュレーション

## Phase1 の範囲

* 取引所: GMOコイン
* 通貨: BTC
* Public API のみ利用
* Kotlin CLI アプリ
* ローカル Docker / docker compose で実行
* devcontainer 上で build / run できること
* 現在価格と過去足データを取得すること
* 判定結果をコンソール出力すること
* CSV 保存すること
* 状態ファイル(JSON)を保存すること

## 技術方針

* 言語: Kotlin
* 取引系は Kotlin
* 将来の分析用だけ Python 追加検討
* ビルド: Gradle Kotlin DSL
* HTTP通信: Ktor Client
* JSON: kotlinx.serialization
* ログ: Kotlin Logging + logback
* 設定ファイル: YAML
* 状態ファイル: JSON

## ディレクトリ構成

最低限、以下の方針。

```text
.
├─ .devcontainer/
├─ docker/
│  └─ app/
│     └─ Dockerfile
├─ config/
│  └─ application.yaml
├─ data/
├─ docs/
├─ src/
├─ build.gradle.kts
├─ docker-compose.yml
└─ README.md
```

## パッケージ構成

* `config`
* `client`
* `service`
* `model`
* `output`

## 処理分割

* データ取得
* 判定
* 出力
* 保存

## 実行仕様

* 5分ごとの定期実行
* アプリ自身が5分ごとに動く
* 5分足ベース
* 直近12本の5分足を使う

## 売買ロジック

* 初期元本: 10,000円
* 1回売買金額: 1,000円固定
* 1ポジションのみ
* 買い候補: 0.5%下落
* 売り候補: 0.5%上昇
* 見送り条件:
  * 直近1時間の変動が 0.3%未満
  * 直近15分で 1.0%以上下落
  * 直近15分で 1.0%以上上昇
* 判定結果:
  * 買い候補
  * 売り候補
  * 見送り
  * 保有中

## 出力仕様

* コンソール出力あり
* 通常時は簡単表示
* サイン時だけ詳しく表示
* 表示項目:
  * 現在価格
  * 損益
  * 売買サイン
  * 理由
  * 想定損益

## CSV仕様

* 保存先: `./data`
* 1日1ファイル
* 列:
  * 日時
  * 価格
  * 売買サイン
  * 理由
  * 損益
  * 保有状態
  * 手数料

## 状態ファイル仕様

* 保存先: `./data/state.json`
* 形式: JSON
* 内容:
  * 保有中かどうか
  * 買値
  * 保有数量
  * 最終更新日時

## 設定ファイル仕様

* 保存先: `./config/application.yaml`
* 形式: YAML
* セクション:
  * `app`
  * `trading`
  * `api`
  * `output`
* 毎回実行時に読み直す
* 条件値は設定ファイルで変更可能

## エラー処理

* API失敗時は3回リトライ
* 3回連続失敗で停止
* エラーログには以下を含める
  * エラー内容
  * 発生場所
  * 入力値

## 受け入れ条件

* devcontainer で開発できる
* `./gradlew build` が通る
* `./gradlew run` が通る
* GMO Public API から ticker / klines を取得できる
* 判定処理が動く
* CSV出力される
* 状態ファイルが出力される
* `docker compose up --build` で起動確認できる
