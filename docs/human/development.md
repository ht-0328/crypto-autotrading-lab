# 開発環境のセットアップ

## 開発環境の前提

* **VS Code Dev Containers** を利用した開発を推奨します。
* Kotlin の開発支援ツールとして、VS Code拡張機能の `Kotlin/kotlin-lsp` (ID: `JetBrains.kotlin`) の利用を推奨します。
  * ※ Marketplaceに見当たらない場合は、公式のReleasesページからVSIXをダウンロードして手動インストールしてください。
* アプリケーション本体のソースコードは `projects/crypto-autotrading-app/` 配下に配置されています。
* 今回のプロジェクトは Kotlin CLI アプリケーションであり、Spring Boot は使用していません。

## GitHub CLI (gh) を用いた Variables 管理手順

開発コンテナには GitHub CLI (`gh`) がインストールされています。これを用いて GitHub Actions の Variables などをローカルから登録・確認することができます。

### 1. 認証 (ログイン)

開発コンテナ内で以下のコマンドを実行し、ブラウザ認証などを経てログインします。

```bash
gh auth login
```

※ ログイン状態は `gh auth status` で確認できます。

### 2. Variables の管理

認証完了後、リポジトリの Variables を操作できます。

Variables の一覧確認:
```bash
gh variable list
```

Variables の登録・更新 (単一の値を設定する場合):
```bash
export VARIABLE_NAME="MY_VAR"
export VARIABLE_VALUE="my_value"
gh variable set $VARIABLE_NAME --body "$VARIABLE_VALUE"
```

ファイルを使って設定する場合:
1. プロジェクトルートに一時ファイル（例: `github-variables.env`）を作成します。
2. 以下のコマンドで登録します。

```bash
gh variable set $VARIABLE_NAME < github-variables.env
```

**【注意事項】**
* Personal Access Token (PAT) やAPIキー、認証情報などの**シークレット情報は絶対にファイル（`github-variables.env` 等）に書き込まない**でください。
* `github-variables.env` は `.gitignore` に登録されていますが、誤ってコミットしないよう十分注意してください。

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
