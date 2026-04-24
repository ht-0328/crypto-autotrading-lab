# crypto-autotrading-lab

仮想通貨の自動売買アプリを段階的に開発するためのリポジトリです。
最終目的は自動化による利益獲得ですが、初期段階（Phase1）は安全を優先し、実注文なしのシミュレーション環境を構築します。

## Phase1 の目標

Phase1では以下の機能を持つKotlin CLIアプリを作成します：
* 取引所: GMOコイン (BTC)
* 現在価格と過去足データの取得
* 売買判定結果のコンソール出力
* 実行結果のCSV保存と状態のJSON保存
* ローカルDocker環境での定期実行 (5分ごと)

## ドキュメント

各仕様や開発手順については以下のドキュメントを参照してください。

### 仕様書
* [Phase1 仕様書](docs/specs/phase1.md)
* [ロードマップ (Phase2以降)](docs/specs/roadmap.md)

### AI依頼前チェック
* [AI依頼テンプレート](docs/ai/prompt-template.md)
* [PRレビュー チェックリスト](docs/ai/review-checklist.md)

### 開発環境
* [開発環境セットアップ手順](docs/setup/development.md)

## 起動方法 (ローカル実行)

本番デプロイ用ではなく、ローカルでの実行確認用として以下のコマンドで起動できます。

GMO Public API で実行する場合:

```bash
cd /workspace/projects/crypto-autotrading-app
./gradlew run
# または明示的に指定する場合
# APP_CONFIG_PATH=/workspace/config/application-gmo.yaml ./gradlew run
```

WireMock で実行する場合:

```bash
cd /workspace/projects/crypto-autotrading-app
APP_CONFIG_PATH=/workspace/config/application-wiremock.yaml ./gradlew run
```

Docker Compose を使用する場合:

```bash
docker compose -f docker/compose/local.yml up --build
```

## WireMock の動作確認

devcontainer 環境では、GMOコインのAPIをモックするWireMockコンテナが起動します。
ローカルのターミナルから以下の `curl` コマンドでモックレスポンスを確認できます。

```bash
# Ticker の確認
curl -s http://localhost:8080/public/v1/ticker?symbol=BTC

# Klines の確認
curl -s "http://localhost:8080/public/v1/klines?symbol=BTC&interval=5min&date=20231001"
```
