# 2. gcloud CLI の準備とログイン

## 文書の目的

- コマンドでGCPを操作するツール（gcloud CLI）の入れ方
- gcloud CLI でのログイン方法
- 使うプロジェクトの指定方法

## 対象読者

運用インフラ構築担当者

## 関連ドキュメント

- [03-workload-identity-federation.md](03-workload-identity-federation.md)

## 1. gcloud CLI をインストールする

`gcloud` CLI は、黒い画面（ターミナル）から Google Cloud を操作するための公式ツールです。

- [公式のインストール手順](https://cloud.google.com/sdk/docs/install) に従ってインストールしてください。
- インストールが終わったら、ターミナルで `gcloud --version` と打ち、バージョンが表示されるか確認します。

## 2. ログインする

ターミナルを開き、以下のコマンドを打ってログインします。

```bash
gcloud auth login
```

ブラウザが開くので、GCPプロジェクトを作ったGoogleアカウントを選んで許可してください。

## 3. 使うプロジェクトを設定する

これからコマンドで操作するプロジェクトを指定します。
以下の `<YOUR_GCP_PROJECT_ID>` の部分を、あなたのプロジェクト ID に書き換えて実行してください。

```bash
export PROJECT_ID="<YOUR_GCP_PROJECT_ID>"
gcloud config set project "$PROJECT_ID"
```

正しく設定されたか確認します。

```bash
gcloud config get-value project
```

_結果にあなたのプロジェクト ID が表示されればOKです。_

## 4. プロジェクトと課金の状態を確認する

プロジェクトが動いていて、課金設定ができているかコマンドで確認します。

```bash
# プロジェクトの状態確認
gcloud projects describe "$PROJECT_ID" \
  --format="table(projectId,projectNumber,lifecycleState)"

# 課金の状態確認
gcloud billing projects describe "$PROJECT_ID" \
  --format="table(billingAccountName,billingEnabled)"
```

_結果の `lifecycleState` が `ACTIVE` になっていて、`billingEnabled` が `True` になっていればOKです。_

## 完了条件チェックリスト

- [ ] `gcloud auth login` でログインできた
- [ ] `gcloud config get-value project` で対象プロジェクトが表示される
- [ ] プロジェクトが `ACTIVE` で、課金が `True` になっている

終わったら、次は [03-workload-identity-federation.md](03-workload-identity-federation.md) に進んでください。
