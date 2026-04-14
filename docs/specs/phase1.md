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

将来的にアプリ以外のプロジェクト（分析用Pythonスクリプトや別バッチなど）を追加しやすくするため、アプリ本体のソースコードは `projects/crypto-autotrading-app/` 配下に隔離します。また、インフラや開発環境の設定はリポジトリのルートで管理し、責務を明確に分離する方針です。

```text
.
├─ .devcontainer/
├─ .vscode/
├─ config/
│  └─ application.yaml
├─ data/
├─ docker/
│  ├─ app/
│  │  └─ Dockerfile
│  └─ compose/
│     └─ local.yml
├─ docs/
├─ projects/
│  └─ crypto-autotrading-app/
│     ├─ build.gradle.kts
│     ├─ settings.gradle.kts
│     ├─ gradlew
│     ├─ gradlew.bat
│     ├─ gradle/
│     └─ src/
├─ README.md
└─ .gitignore
```

### 主要ディレクトリの役割

| ディレクトリ | 役割 |
|---|---|
| `.devcontainer/` | VS Code Dev Containers の設定ファイル群 |
| `.vscode/` | VS Code ワークスペース設定（拡張機能の推奨など） |
| `config/` | アプリケーションの設定ファイル配置ディレクトリ |
| `data/` | 実行結果のCSVや状態ファイルの保存ディレクトリ |
| `docker/` | Dockerfile や docker compose の設定ファイル群 |
| `docs/` | 仕様書や開発手順などのドキュメント群 |
| `projects/` | `crypto-autotrading-app` などの Kotlin CLI アプリケーションのプロジェクト群を配置するディレクトリ |

## パッケージ構成

アプリ本体のパッケージ構成は以下のようにします。

```text
projects/crypto-autotrading-app/
└─ src/
   └─ main/
      └─ kotlin/
         └─ cryptoautotrading/
            ├─ presentation/
            ├─ application/
            ├─ domain/
            │  ├─ strategy/
            │  ├─ simulation/
            │  └─ model/
            ├─ infrastructure/
            │  ├─ exchange/
            │  │  └─ gmo/
            │  ├─ config/
            │  └─ output/
            └─ shared/
```

| パッケージ                         | 役割                                |
| ----------------------------- | --------------------------------- |
| `presentation`                | `main` 関数、CLI起動処理                 |
| `application`                 | 5分ごとの実行制御、1回分の処理フローの組み立て          |
| `domain.strategy`             | 買い候補、売り候補、見送り、保有中などの判定ルール         |
| `domain.simulation`           | 仮想資金、保有状態、損益計算、シミュレーション状態の更新      |
| `domain.model`                | 売買判定結果、価格情報、保有状態などの中心データ型         |
| `infrastructure.exchange.gmo` | GMOコイン Public API との通信、APIレスポンス変換 |
| `infrastructure.config`       | `application.yaml` の読み込み、設定値の変換   |
| `infrastructure.output`       | コンソール出力、CSV出力、`state.json` 保存     |
| `shared`                      | 共通例外、共通ユーティリティ、共通定数               |

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

* 保存先: リポジトリルート直下の `data/` ディレクトリ
  * Kotlinアプリからは実行時のカレントディレクトリ基準で参照する
  * Docker実行時も同じパスで扱えるようにする
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

* 保存先: リポジトリルート直下の `data/state.json`
  * Kotlinアプリからは実行時のカレントディレクトリ基準で参照する
  * Docker実行時も同じパスで扱えるようにする
* 形式: JSON
* 内容:
  * 保有中かどうか
  * 買値
  * 保有数量
  * 最終更新日時

## 設定ファイル仕様

* 保存先: リポジトリルート直下の `config/application.yaml`
  * Kotlinアプリからは実行時のカレントディレクトリ基準で参照する
  * Docker実行時も同じパスで扱えるようにする
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
* `cd projects/crypto-autotrading-app && ./gradlew build` が通る
* `cd projects/crypto-autotrading-app && ./gradlew run` が通る
* GMO Public API から ticker / klines を取得できる
* 判定処理が動く
* CSV出力される
* 状態ファイルが出力される
* `docker compose -f docker/compose/local.yml up --build` で起動確認できる
