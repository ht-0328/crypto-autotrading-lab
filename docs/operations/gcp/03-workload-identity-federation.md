# 3. Workload Identity Federation の設定

| 項目 | 内容 |
| --- | --- |
| 想定読者 | GitHub Actions から GCP を操作したい運用担当者 |
| 読んだあとできること | 鍵ファイルを使わずに GitHub Actions から GCP へ認証できる |
| 状態 | 現行 |
| 機密区分 | 公開可 |
| 作成者 | リポジトリ管理者 |
| 保守責任者 | リポジトリ管理者 |
| 最終確認日 | 2026-08-30 |


## 文書の目的

- GitHub Actions から GCP へ安全にアクセスする仕組みの作り方

## 対象読者

運用インフラ構築担当者

## 関連ドキュメント

- [04-service-accounts-and-iam.md](04-service-accounts-and-iam.md)

## 概要

GCP へのデプロイで JSONキーを GitHub に登録する方法は使いません。漏れると危険なためです。
代わりに **Workload Identity Federation** を使います。「このリポジトリから来た通信なら許可する」という設定です。

## 1. 準備（環境変数の設定）

ターミナルで以下のコマンドを実行し、必要な変数（名前）を設定します。
`<YOUR_GITHUB_USER>/<YOUR_GITHUB_REPO>` の部分は、自分のGitHubのURLに合わせて書き換えてください。（例: `ht-0328/crypto-autotrading-lab`）

```bash
export PROJECT_ID="$(gcloud config get-value project)"
export PROJECT_NUMBER="$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')"

# Workload Identity の名前
export WORKLOAD_IDENTITY_POOL="github-actions-pool"
export WORKLOAD_IDENTITY_PROVIDER="github-actions-provider"

# GitHubリポジトリの指定（例: my-org/my-repo）
export GITHUB_REPO="<YOUR_GITHUB_USER>/<YOUR_GITHUB_REPO>"
```

## 2. API を有効にする

この機能を使うためのAPIをオンにします。

```bash
gcloud services enable \
  iamcredentials.googleapis.com \
  iam.googleapis.com
```

## 3. Workload Identity Pool と Provider を作る

GitHub と GCP を繋ぐ「入り口」を作ります。

**Pool（プール）の作成:**

```bash
gcloud iam workload-identity-pools create "$WORKLOAD_IDENTITY_POOL" \
  --project "$PROJECT_ID" \
  --location="global" \
  --display-name="GitHub Actions"
```

**Provider**
ここで「指定したGitHubリポジトリからだけ許可する」という条件（`attribute-condition`）を設定しています。

```bash
gcloud iam workload-identity-pools providers create-oidc "$WORKLOAD_IDENTITY_PROVIDER" \
  --project "$PROJECT_ID" \
  --location="global" \
  --workload-identity-pool="$WORKLOAD_IDENTITY_POOL" \
  --display-name="GitHub Actions Provider" \
  --issuer-uri="https://token.actions.githubusercontent.com" \
  --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository" \
  --attribute-condition="assertion.repository == '${GITHUB_REPO}'"
```

## 4. 設定の確認

正しく作られたか確認します。

```bash
gcloud iam workload-identity-pools providers list \
  --project="$PROJECT_ID" \
  --location="global" \
  --workload-identity-pool="$WORKLOAD_IDENTITY_POOL" \
  --format="table(name,displayName,state)"
```

## 完了条件チェックリスト

- [ ] Pool と Provider をエラーなく作成できた
- [ ] 確認コマンドで一覧に `GitHub Actions Provider` が表示される

終わったら、次は [04-service-accounts-and-iam.md](04-service-accounts-and-iam.md) に進んでください。
