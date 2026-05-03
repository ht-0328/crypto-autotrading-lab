# 4. デプロイ用サービスアカウントの作成と権限（IAM）設定

## この文書で分かること
- GitHub Actions が GCP を操作するための「専用アカウント（サービスアカウント）」の作り方
- そのアカウントに、デプロイに必要な権限（IAMロール）を付ける方法
- Workload Identity とサービスアカウントを繋ぐ方法

## 読む人
運用インフラ構築担当者

## 関連ドキュメント
- [05-github-actions-variables.md](05-github-actions-variables.md)

## まず結論
GitHub Actions が GCP にプログラムをデプロイするには、「デプロイ専用のロボット（サービスアカウント）」を作る必要があります。
そして、そのロボットに「必要な操作だけができる権限」を持たせ、先ほど作った Workload Identity（入り口）と繋げます。

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

```bash
gcloud iam service-accounts create "$DEPLOY_SERVICE_ACCOUNT_NAME" \
  --project "$PROJECT_ID" \
  --display-name="GitHub Actions Deploy Account"
```

## 3. 必要な権限（IAMロール）を付ける
このアカウントに、GCPのリソースを作ったり設定したりする権限を与えます。
※初期構築を簡単にするため、ここでは少し強めの権限を付けています。本番運用では後から絞ることも検討してください。

```bash
ROLES=(
  "roles/serviceusage.serviceUsageAdmin"    # APIをオンにするため
  "roles/artifactregistry.admin"            # Dockerイメージ保存場所を作るため
  "roles/storage.admin"                     # ファイル保存場所を作るため
  "roles/iam.serviceAccountAdmin"           # 実行用のアカウントなどを作るため
  "roles/resourcemanager.projectIamAdmin"   # 権限を設定するため
  "roles/cloudbuild.builds.editor"          # プログラムをビルドするため
  "roles/run.admin"                         # Cloud Runにデプロイするため
  "roles/iam.serviceAccountUser"            # 別のアカウントとして動かすため
)

for ROLE in "${ROLES[@]}"; do
  gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:$DEPLOY_SERVICE_ACCOUNT_EMAIL" \
    --role="$ROLE" \
    --condition=None
done
```

## 4. Workload Identity とサービスアカウントを繋ぐ
「指定したGitHubリポジトリから来た通信」なら「このデプロイ用アカウントとして動いて良い」という設定をします。

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
