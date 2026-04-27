# crypto-autotrading-lab

仮想通貨の自動売買アプリを段階的に開発するためのリポジトリです。
最終目的は自動化による利益獲得ですが、初期段階（Phase1）は安全を優先し、実注文なしのシミュレーション環境を構築します。

## 開発ドキュメント

人間向けの各種仕様や開発手順については、以下のドキュメントを参照してください。

* [プロダクト要求仕様（共通）](docs/human/product-requirements.md)
* [Phase1 仕様書](docs/human/phase1.md)
* [ロードマップ (Phase2以降)](docs/human/roadmap.md)
* [開発フロー](docs/human/development-flow.md)
* [開発環境セットアップ手順](docs/human/development.md)
* [GCP デプロイ手順](docs/human/gcp-deployment.md)

## リポジトリ構成

主要ディレクトリの役割は以下です。

* `projects/crypto-autotrading-app/`: Kotlin CLI アプリ本体
* `config/`: 実行環境ごとの設定ファイル（GMO API / WireMock）
* `mocks/wiremock/`: WireMock のスタブ定義
* `docker/`: ローカル実行用の Dockerfile / Compose 定義
* `docs/human/`: 人間向けの仕様・手順書
* `docs/ai/`: AIエージェント向けのプロンプト・制約ルール

## 起動方法 (ローカル実行)

ローカルでの実行確認用として以下のコマンドで起動できます。

```bash
cd projects/crypto-autotrading-app

# アプリの実行 (デフォルト設定)
./gradlew run

# テストの実行
./gradlew test
```

Docker Compose を使用する場合:

```bash
docker compose -f docker/compose/local.yml up --build
```
