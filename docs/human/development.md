# 開発環境のセットアップ

## 概要
開発環境のセットアップと実行手順

## 対象読者
開発メンバー

## この文書で分かること
VS Code Dev ContainersやDockerを利用したローカル開発環境の構築手順

## 関連ドキュメント
[gcp-deployment.md](gcp-deployment.md)

## 前提
本ドキュメントの記載内容は、Phase1（シミュレーション環境）を前提としています。

## 本文
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

ファイルを使って一括登録する場合:
1. プロジェクトルートに一時ファイル（例: `github-variables.env`）を作成し、`キー=値` の形式で Variables を記述します。
2. 以下のコマンドで一括登録します。

```bash
gh variable set -f github-variables.env --repo ht-0328/crypto-autotrading-lab
```

3. 登録が完了したか、一覧表示コマンドで確認します。

```bash
gh variable list --repo ht-0328/crypto-autotrading-lab
```

4. 登録・確認完了後、一時ファイル `github-variables.env` を削除します。

**【注意事項】**
* `github-variables.env` はGit管理しない一時ファイルです。`.gitignore` に登録されていますが、誤ってコミットしないよう十分注意してください。
* Personal Access Token (PAT) やAPIキー、認証情報などの**シークレット情報は絶対にファイル（`github-variables.env` 等）に書き込まない**でください。

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

## 実行形態の前提

本アプリケーションは以下の実行形態をサポートしています。

- **ローカルGradle実行**: 手軽な動作確認
- **Docker Compose実行**: 他の依存関係を含む独立環境での確認
- **GitHub Actions CI**: 自動テスト・静的解析の実行
- **GCP Cloud Run Jobデプロイ**: クラウド上でのシミュレーション運用

※ Phase1においては、GCP環境にデプロイした場合でも実注文は行わず、あくまでシミュレーション実行のみを行う設計としています。


## 注意点
特にありません。

## 更新タイミング
システムの要件や運用フローが変更された際に更新してください。