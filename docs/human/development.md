# 開発環境のセットアップ

## 開発環境の前提

* **VS Code Dev Containers** を利用した開発を推奨します。
* Kotlin の開発支援ツールとして、VS Code拡張機能の `Kotlin/kotlin-lsp` (ID: `JetBrains.kotlin`) の利用を推奨します。
  * ※ Marketplaceに見当たらない場合は、公式のReleasesページからVSIXをダウンロードして手動インストールしてください。
* アプリケーション本体のソースコードは `projects/crypto-autotrading-app/` 配下に配置されています。
* 今回のプロジェクトは Kotlin CLI アプリケーションであり、Spring Boot は使用していません。

## アプリケーションの実行・テスト手順

テストを実行する場合は以下のコマンドを使用します（Java 17がGradle Toolchainsにより自動解決されます）:

```bash
cd projects/crypto-autotrading-app
./gradlew test
```

アプリケーションをローカル実行する場合は以下のコマンドを使用します:

```bash
cd projects/crypto-autotrading-app
./gradlew build
./gradlew run
```

Docker Compose を利用してアプリケーションを起動する場合:

```bash
docker compose -f docker/compose/local.yml up --build
```
※ `docker/compose/local.yml` はアプリケーションコンテナ起動用の定義です。WireMockを用いたローカルモック環境を利用する場合は、devcontainer内のWireMockに接続するか、別途起動してください。

## 実行環境の将来方針

* 初期段階はローカルの Docker コンテナ上での実行を想定しています。
* 将来的にはクラウド環境（AWS, GCP, レンタルサーバー等）へデプロイしやすい構成を目指しますが、現時点ではクラウド対応の実装は不要です。
