# 4. デプロイ用サービスアカウントの作成と権限（IAM）設定

| 項目 | 内容 |
| --- | --- |
| 想定読者 | デプロイ用の権限を設定する運用担当者 |
| 読んだあとできること | デプロイに必要な最小限の権限を持つサービスアカウントを作れる |
| 状態 | 現行 |
| 機密区分 | 公開可 |
| 作成者 | リポジトリ管理者 |
| 保守責任者 | リポジトリ管理者 |
| 最終確認日 | 2026-08-30 |


## 文書の目的

- GitHub Actions が GCP を操作するための専用アカウントの作り方
- そのアカウントに、デプロイに必要な権限（IAMロール）を付ける方法
- Workload Identity とサービスアカウントを繋ぐ方法

## 対象読者

運用インフラ構築担当者

## 関連ドキュメント

- [05-github-actions-variables.md](05-github-actions-variables.md)

## 概要

GitHub Actions が GCP へデプロイするには、専用のロボットが要ります。これをサービスアカウントと呼びます。
そのロボットに「必要な操作だけができる権限」を持たせます。作成済みの Workload Identity と繋げます。

## 1. 準備（環境変数の設定）

引き続きターミナルで作業します。以下の変数を設定してください。

```bash
export PROJECT_ID="$(gcloud config get-value project)"
export PROJECT_NUMBER="$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')"
export WORKLOAD_IDENTITY_POOL="github-actions-pool"
export GITHUB_REPO="<YOUR_GITHUB_USER>/<YOUR_GITHUB_REPO>"

# デプロイ用アカウントの名前とメールアドレス
export DEPLOY_SERVICE_ACCOUNT_NAME="github-actions-deploy"
export DEPLOY_SERVICE_ACCOUNT_EMAIL="${DEPLOY_SERVICE_ACCOUNT_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"
```

## 2. サービスアカウントを作る

デプロイ専用のアカウントを作ります。
自動化スクリプトで存在確認をする場合は、`describe` ではなく `list` を使います。

**理由:**
`describe` は曖昧なエラーを返すことがあります。対象が無い場合や削除直後に `PERMISSION_DENIED ... or it may not exist` を返し、自動化ワークフローが誤って停止します。そのため、`list` コマンドでフィルタリングして結果が空になるかを確認する方針（`gcloud iam service-accounts list --filter="email:${SA_EMAIL}" --format="value(email)"`）を採っています。

```bash
gcloud iam service-accounts create "$DEPLOY_SERVICE_ACCOUNT_NAME" \
  --project "$PROJECT_ID" \
  --display-name="GitHub Actions Deploy Account"
```

## 3. 必要な権限（IAMロール）を付ける

このアカウントに、GCPのリソースを作ったり設定したりする権限を与えます。
初期構築（Bootstrap）時には、リソース作成のための強い権限が必要です。
通常デプロイ（Deploy）の段階では、権限を最小限に絞ることを推奨します。

### 通常デプロイ用の最小権限構成

通常デプロイではリソースを作成しません。既存リソースの利用と更新だけなので、以下の権限で足ります。

```bash
ROLES=(
  "roles/cloudbuild.builds.editor"          # プログラムをビルドするため
  "roles/run.developer"                     # Cloud Runにデプロイするため (adminより弱い権限)
  "roles/serviceusage.serviceUsageConsumer" # 既存のAPIを利用するため
  "roles/iam.serviceAccountViewer"          # サービスアカウントの存在確認を行うため
)

for ROLE in "${ROLES[@]}"; do
  gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:$DEPLOY_SERVICE_ACCOUNT_EMAIL" \
    --role="$ROLE" \
    --condition=None
done
```

### 対象サービスアカウントへの `iam.serviceAccountUser` 付与

さらに権限を絞ります。`roles/iam.serviceAccountUser` は、デプロイで使う特定のサービスアカウントにだけ付与します。

対象となるサービスアカウントの例：

- `cloud-build-builder@<PROJECT_ID>.iam.gserviceaccount.com`
- `crypto-autotrading-lab-runner@<PROJECT_ID>.iam.gserviceaccount.com`

```bash
# Cloud Build 用サービスアカウントへの付与
gcloud iam service-accounts add-iam-policy-binding "cloud-build-builder@${PROJECT_ID}.iam.gserviceaccount.com" \
  --member="serviceAccount:$DEPLOY_SERVICE_ACCOUNT_EMAIL" \
  --role="roles/iam.serviceAccountUser"

# Cloud Run 実行用サービスアカウントへの付与
gcloud iam service-accounts add-iam-policy-binding "crypto-autotrading-lab-runner@${PROJECT_ID}.iam.gserviceaccount.com" \
  --member="serviceAccount:$DEPLOY_SERVICE_ACCOUNT_EMAIL" \
  --role="roles/iam.serviceAccountUser"
```

!!! note "初期構築時に必要な一時的な権限"

    初期構築のワークフローを初めて実行するときだけ、一時的に強い権限が必要になる場合があります。
    対象は `bootstrap-create-gcp.yml` と `bootstrap-grant-iam.yml` です。
    必要になりうる権限は `roles/iam.serviceAccountAdmin`、`roles/resourcemanager.projectIamAdmin`、`roles/storage.admin`、`roles/artifactregistry.admin` です。

    動作確認のため、`github-actions-deployer` に一時的に強めの権限を残しています。
    Bootstrap Create → Grant IAM → Deploy が main で安定して通ることを確認します。
    そのあと、別作業でこの権限を段階的に削減します。

## 4. Workload Identity とサービスアカウントを繋ぐ

「指定したリポジトリから来た通信」に、「このアカウントとして動いてよい」と許可します。

```bash
gcloud iam service-accounts add-iam-policy-binding "$DEPLOY_SERVICE_ACCOUNT_EMAIL" \
  --project "$PROJECT_ID" \
  --role="roles/iam.workloadIdentityUser" \
  --member="principalSet://iam.googleapis.com/projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/${WORKLOAD_IDENTITY_POOL}/attribute.repository/${GITHUB_REPO}"
```

## 5. 設定の確認

アカウントに正しい権限がついているか確認します。

```bash
gcloud projects get-iam-policy "$PROJECT_ID" \
  --flatten="bindings[].members" \
  --filter="bindings.members:serviceAccount:${DEPLOY_SERVICE_ACCOUNT_EMAIL}" \
  --format="table(bindings.role)"
```

## 完了条件チェックリスト

- [ ] サービスアカウントを作れた
- [ ] 必要なIAMロールをすべて付けられた
- [ ] Workload Identity との紐づけ（バインディング）ができた

終わったら、次は [05-github-actions-variables.md](05-github-actions-variables.md) に進んでください。
