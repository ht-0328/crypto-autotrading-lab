# 開発環境のセットアップ

## 文書の目的

- 開発環境（VS Code Dev Containers）の準備
- GitHub CLIを使った設定値（Variables）の登録方法
- アプリケーションのテストと実行の手順
- プログラムを動かす様々な方法

## 対象読者

開発メンバー

## 関連ドキュメント

- [gcp/06-deploy-cloud-run-job.md](../operations/gcp/06-deploy-cloud-run-job.md)

## 概要

このプロジェクトでは、自分のパソコンの環境を汚さないように **VS Code Dev Containers**（Dockerを使った開発環境）を使うことをお勧めします。
プログラムのコードは `projects/crypto-autotrading-app/` フォルダに入っています。

## 開発環境の準備

1. **VS Code** と **Docker** をインストールします。
2. VS Code でこのプロジェクトを開き、右下に出る「Reopen in Container」をクリックして、Dev Container を立ち上げます。
3. Kotlinを書くために、VS Code拡張機能の `Kotlin/kotlin-lsp` を入れると便利です。

## GitHub CLI (gh) を使った設定の管理

開発環境には、コマンドからGitHubを操作できる `gh` コマンドが入っています。これを使って、GCPにデプロイするための設定値（Variables）を登録できます。

### 1. ログインする

ターミナルで以下のコマンドを打ち、ブラウザを開いてログインします。

```bash
gh auth login
```

### 2. 設定値（Variables）を確認・登録する

今登録されている設定値を見るには：

```bash
gh variable list --repo ht-0328/crypto-autotrading-lab
```

1つだけ新しい設定値を登録するには：

```bash
export VARIABLE_NAME="MY_VAR"
export VARIABLE_VALUE="my_value"
gh variable set $VARIABLE_NAME --body "$VARIABLE_VALUE" --repo ht-0328/crypto-autotrading-lab
```

**【注意】** APIキーやパスワードなどの**秘密情報は、このコマンド（Variables）ではなく、GitHubの画面から「Secrets」として登録**してください。

## アプリケーションの実行・テスト手順

プログラムのテストや実行は、ターミナルからコマンドを打って行います。

**テストを実行する:**

```bash
cd projects/crypto-autotrading-app
./gradlew test
```

**ローカルでプログラムを動かす:**

```bash
cd projects/crypto-autotrading-app
./gradlew build
./gradlew run
```

**Docker を使って動かす:**

```bash
docker compose -f docker/compose/local.yml up --build
```

## プログラムを動かす4つの方法

このアプリは、目的に合わせて4つの方法で動かすことができます。Phase1ではどの方法でも実際の注文はしません。

1. **ローカルGradle実行**: パソコン上で手軽に動かして確認したいとき。
2. **Docker Compose実行**: 本番に近い環境で動かしたいとき。
3. **GitHub Actions CI**: コードを変更したときに、自動でテストしたいとき。
4. **GCP Cloud Run Job**: クラウド上で定期的にシミュレーションを動かしたいとき。

## ドキュメントサイト（Zensical）

`docs/` 配下の Markdown は [Zensical](https://zensical.org/) で静的サイト化し、GitHub Pages で公開しています。

- 公開先: [crypto-autotrading-lab ドキュメント](https://ht-0328.github.io/crypto-autotrading-lab/)
- サイトの設定: [zensical.toml](../../zensical.toml)（章立て・テーマ・Markdown 拡張）
- 公開の仕組み: [.github/workflows/docs.yml](../../.github/workflows/docs.yml)

`main` への push で自動的にビルドとデプロイが走ります。PR では公開せず、ビルドが通るかだけを確認します。

### 手元で見た目を確認する

Python 3.10 以上が必要です。

**DevContainer には Python の仮想環境モジュールが入っていません。** DevContainer の中で作業する場合は、先に次を実行してください（1回だけ）。

```bash
apt-get update && apt-get install -y python3-venv
```

そのうえで、仮想環境を作って Zensical をインストールします。

```bash
# リポジトリのルートで実行する
python3 -m venv .venv
.venv/bin/pip install zensical==0.0.57

# http://localhost:8000 で確認できる。Markdown を保存すると自動で再ビルドされる
.venv/bin/zensical serve
```

!!! note "見た目の確認は必須ではありません"

    Zensical をインストールしなくても、PR を出せば `Documentation` チェックがビルドを検証します。手元で確認したいときだけ入れてください。

静的ファイルだけを出力したい場合は `zensical build` を使います。出力先の `site/` はコミットしません（[.gitignore](../../.gitignore) で除外済み）。

### 章立てを変えるとき

ドキュメントを追加・移動・削除したら、[zensical.toml](../../zensical.toml) の `nav` も更新してください。`nav` に書かれていないファイルはサイトの目次に出ません。

### 現時点の既知の問題

ビルドは成功しますが、`docs/` の外を指す相対リンク（リポジトリ直下の Markdown、Kotlin のソースコード、ワークフロー定義など）はサイト上でリンク切れになります。GitHub 上で Markdown を直接読む分には正しく動くリンクです。

解消は別途対応します。ドキュメントを新しく書くときは、`docs/` の外を参照する場合に GitHub の URL を使ってください。

## 用語補足

- **Dev Containers**: パソコンの中に「開発専用の小さなパソコン（コンテナ）」を作って、そこで作業する仕組み。
- **Variables**: プログラムを動かすための設定値。
- **Secrets**: APIキーなど、人に見られてはいけない秘密の情報。

## 更新方針

システムの要件や運用フローが変更された際に更新してください。
