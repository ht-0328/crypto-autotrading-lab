# GitHub Actions から GCP にデプロイするための準備

## このドキュメントの目的
このドキュメントは、GitHub Actions を使って GCP (Google Cloud Platform) へアプリケーションをデプロイするための準備手順を説明します。

- まだ GCP アカウントの作成、GCP プロジェクトの作成、課金設定が終わっていない場合は、先に `docs/human/gcp-account-and-project-setup.md` を読んで完了させてください。
- このドキュメントの準備を完了すると、最終的には GitHub Actions の workflow を実行するだけで、Cloud Build でのビルドと Cloud Run Job へのデプロイが自動でできるようになります。
- ただし、GitHub Actions が GCP に安全にアクセスするための「認証の入り口」は、最初に手動で作成する必要があります。

## 全体像
GitHub Actions から GCP にデプロイする流れは以下のようになります。

```text
GitHub Actions
↓
Workload Identity Federation
↓
デプロイ用サービスアカウント
↓
Cloud Build で Docker イメージ作成
↓
Artifact Registry に Docker イメージ保存
↓
Cloud Run Job にデプロイ
↓
GCS をマウントしてログやデータを保存
```

**各要素の役割:**
- **GitHub Actions**: GitHub 上でソースコードのビルドやデプロイなどの自動処理を実行する仕組みです。
- **Workload Identity Federation**: GitHub Actions からパスワード（シークレットキー）を使わずに GCP に安全にログインするための仕組みです。
- **サービスアカウント**: プログラムやシステムが GCP を操作するために使う「プログラム用のユーザー」のようなものです。
- **Cloud Build**: Docker イメージを作ったり、ビルド作業を行う GCP のサービスです。
- **Artifact Registry**: 作成した Docker イメージを保存しておく保管場所です。
- **Cloud Run Job**: コンテナ化されたバッチ処理を実行するサービスです。
- **GCS (Google Cloud Storage)**: ファイルを保存するストレージです。アプリケーションのログや設定ファイルを置くために使います。

## 事前に必要なもの
これらは、今回の作業を始める前に揃っている必要があります。
- **GCP プロジェクト**: リソースを作成する器です。
- **課金設定**: 有効になっていないと GCP サービスが利用できません。
- **gcloud CLI**: 手元の PC から GCP を操作するためのコマンドツールです。
- **GitHub リポジトリ**: デプロイ対象のソースコードが置かれている場所です。
- **GitHub Actions が有効であること**: リポジトリの設定で Actions が許可されている必要があります。

GCP プロジェクトや課金設定については、詳細は [GCP アカウントとプロジェクトのセットアップ](gcp-account-and-project-setup.md) を参照してください。

## 事前に作らなくてよいもの
以下のリソースは、今後 GitHub Actions の workflow (`deploy-gcp.yml`) 側で自動作成できるため、手動で作る必要はありません。（現時点で workflow がまだ自動作成に対応していない場合でも、今後の改善として自動化される前提で進めます）。

- Artifact Registry リポジトリ
- GCS バケット
- Cloud Build 用サービスアカウント
- Cloud Run 実行用サービスアカウント
- Cloud Run Job
- 必要 API の有効化（今回は初回の権限設定のため手動で有効化します）
- 各種 IAM ロール付与

現在の workflow では一部の GCP リソースが事前作成されている前提になっています。
今後 `deploy-gcp.yml` に setup 処理を追加することで、Artifact Registry、GCS、サービスアカウント、IAM 付与などを完全に自動化できます。

## 使用する値の考え方
Public（公開）リポジトリのドキュメントには、実際の GCP プロジェクト ID やサービスアカウント名をそのまま書かないでください。
本ドキュメントでは、以下のような「プレースホルダー（置き換え用の文字列）」を使用しています。実際にコマンドを実行する際は、ご自身の環境に合わせて書き換えてください。

| 項目 | 値の例・説明 |
| --- | --- |
| GCPプロジェクトID | `<YOUR_GCP_PROJECT_ID>` |
| GCPリージョン | `<YOUR_GCP_REGION>`。東京リージョンなら `asia-northeast1` |
| Artifact Registry名 | `<YOUR_ARTIFACT_REGISTRY_REPOSITORY_NAME>` |
| Dockerイメージ名 | `<YOUR_IMAGE_NAME>` |
| GCSバケット名 | `<YOUR_GCS_BUCKET_NAME>` |
| Cloud Run Job名 | `<YOUR_CLOUD_RUN_JOB_NAME>` |
| デプロイ用サービスアカウント名 | `<YOUR_DEPLOY_SERVICE_ACCOUNT_NAME>` |
| Cloud Build用サービスアカウント名 | `<YOUR_BUILD_SERVICE_ACCOUNT_NAME>` |
| Cloud Run実行用サービスアカウント名 | `<YOUR_RUNTIME_SERVICE_ACCOUNT_NAME>` |
| Workload Identity Pool名 | `<YOUR_WORKLOAD_IDENTITY_POOL_NAME>` |
| Workload Identity Provider名 | `<YOUR_WORKLOAD_IDENTITY_PROVIDER_NAME>` |
| GitHubリポジトリ | `<YOUR_GITHUB_OWNER>/<YOUR_GITHUB_REPOSITORY>` (例: `ht-0328/crypto-autotrading-lab`) |

---

## 1. 最初に設定する環境変数
ターミナルを開き、以下の環境変数を設定します。
`<YOUR_...>` の部分をご自身の値に書き換えてから実行してください。

```bash
export PROJECT_ID="<YOUR_GCP_PROJECT_ID>"
export REGION="<YOUR_GCP_REGION>"
export GITHUB_REPO="<YOUR_GITHUB_OWNER>/<YOUR_GITHUB_REPOSITORY>"

export ARTIFACT_REPOSITORY="<YOUR_ARTIFACT_REGISTRY_REPOSITORY_NAME>"
export IMAGE_NAME="<YOUR_IMAGE_NAME>"
export GCS_BUCKET_NAME="<YOUR_GCS_BUCKET_NAME>"
export CLOUD_RUN_JOB_NAME="<YOUR_CLOUD_RUN_JOB_NAME>"

export DEPLOY_SERVICE_ACCOUNT_NAME="<YOUR_DEPLOY_SERVICE_ACCOUNT_NAME>"
export BUILD_SERVICE_ACCOUNT_NAME="<YOUR_BUILD_SERVICE_ACCOUNT_NAME>"
export RUNTIME_SERVICE_ACCOUNT_NAME="<YOUR_RUNTIME_SERVICE_ACCOUNT_NAME>"

export WORKLOAD_IDENTITY_POOL="<YOUR_WORKLOAD_IDENTITY_POOL_NAME>"
export WORKLOAD_IDENTITY_PROVIDER="<YOUR_WORKLOAD_IDENTITY_PROVIDER_NAME>"

# 以下の変数は上の設定から自動で計算されます
export PROJECT_NUMBER="$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')"

export DEPLOY_SERVICE_ACCOUNT_EMAIL="${DEPLOY_SERVICE_ACCOUNT_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"
export BUILD_SERVICE_ACCOUNT_EMAIL="${BUILD_SERVICE_ACCOUNT_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"
export RUNTIME_SERVICE_ACCOUNT_EMAIL="${RUNTIME_SERVICE_ACCOUNT_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"
```

## 2. GCP プロジェクトと課金の確認手順
操作対象のプロジェクトが正しいか、有効になっているかを確認します。

```bash
gcloud config get-value project

gcloud projects describe "$PROJECT_ID" \
  --format="table(projectId,projectNumber,lifecycleState)"

gcloud billing projects describe "$PROJECT_ID" \
  --format="table(billingAccountName,billingEnabled)"
```

**期待する状態**:
- `lifecycleState` が `ACTIVE`
- `billingEnabled` が `True`

## 3. 必要 API の有効化手順
デプロイやリソース作成に必要な GCP の API を有効化します。

```bash
gcloud services enable \
  serviceusage.googleapis.com \
  iam.googleapis.com \
  iamcredentials.googleapis.com \
  cloudbuild.googleapis.com \
  artifactregistry.googleapis.com \
  run.googleapis.com \
  storage.googleapis.com \
  --project "$PROJECT_ID"
```

有効化されたか確認します。

```bash
gcloud services list \
  --enabled \
  --project "$PROJECT_ID" \
  --filter="NAME:(serviceusage.googleapis.com OR iam.googleapis.com OR iamcredentials.googleapis.com OR cloudbuild.googleapis.com OR artifactregistry.googleapis.com OR run.googleapis.com OR storage.googleapis.com)" \
  --format="table(NAME,TITLE)"
```

## 4. デプロイ用サービスアカウントの作成手順
GitHub Actions が GCP を操作する際に使用する「デプロイ用」のサービスアカウントを作成します。

```bash
gcloud iam service-accounts create "$DEPLOY_SERVICE_ACCOUNT_NAME" \
  --project "$PROJECT_ID" \
  --display-name="GitHub Actions Deployer"
```

作成できたか確認します。

```bash
gcloud iam service-accounts describe "$DEPLOY_SERVICE_ACCOUNT_EMAIL" \
  --project "$PROJECT_ID" \
  --format="table(email,displayName,disabled)"
```

## 5. デプロイ用サービスアカウントへの IAM 付与手順
作成したサービスアカウントに、リソースの作成やデプロイに必要な権限（IAM ロール）を付与します。

```bash
for ROLE in \
  "roles/serviceusage.serviceUsageAdmin" \
  "roles/artifactregistry.admin" \
  "roles/storage.admin" \
  "roles/iam.serviceAccountAdmin" \
  "roles/resourcemanager.projectIamAdmin" \
  "roles/cloudbuild.builds.editor" \
  "roles/run.admin" \
  "roles/iam.serviceAccountUser"
do
  gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:${DEPLOY_SERVICE_ACCOUNT_EMAIL}" \
    --role="$ROLE"
done
```

**各ロールの役割**:
| ロール | 役割 |
| --- | --- |
| `serviceusage.serviceUsageAdmin` | API の有効化・無効化を行うため |
| `artifactregistry.admin` | Artifact Registry リポジトリを作成・管理するため |
| `storage.admin` | GCS バケットを作成・管理するため |
| `iam.serviceAccountAdmin` | Cloud Build用や実行用のサービスアカウントを作成するため |
| `resourcemanager.projectIamAdmin` | 作成したサービスアカウントに権限を付与するため |
| `cloudbuild.builds.editor` | Cloud Build を実行して Docker イメージを作成するため |
| `run.admin` | Cloud Run Job を作成・更新するため |
| `iam.serviceAccountUser` | Cloud Run 実行時に他のサービスアカウントとして振る舞うため |

> **注意**: ここで付与している権限は、初期構築を簡単にするためにやや強めです。
> 個人開発や検証では扱いやすいですが、本番運用では後から最小権限に絞る余地があります。

## 6. Workload Identity Federation の作成手順
GitHub Actions が安全にデプロイ用サービスアカウントにアクセスできるように設定します。

**Workload Identity Pool の作成**
```bash
gcloud iam workload-identity-pools create "$WORKLOAD_IDENTITY_POOL" \
  --project "$PROJECT_ID" \
  --location="global" \
  --display-name="GitHub Actions"
```

**Provider の作成** (GitHub との連携設定)
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

**デプロイ用サービスアカウントへのアクセス権付与**
```bash
gcloud iam service-accounts add-iam-policy-binding "$DEPLOY_SERVICE_ACCOUNT_EMAIL" \
  --project "$PROJECT_ID" \
  --role="roles/iam.workloadIdentityUser" \
  --member="principalSet://iam.googleapis.com/projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/${WORKLOAD_IDENTITY_POOL}/attribute.repository/${GITHUB_REPO}"
```

**確認コマンド**
```bash
# Provider の確認
gcloud iam workload-identity-pools providers list \
  --project="$PROJECT_ID" \
  --location="global" \
  --workload-identity-pool="$WORKLOAD_IDENTITY_POOL" \
  --format="table(name,displayName,state)"

# バインディングの確認
gcloud iam service-accounts get-iam-policy "$DEPLOY_SERVICE_ACCOUNT_EMAIL" \
  --project "$PROJECT_ID" \
  --format="table(bindings.role,bindings.members)"
```

## 7. GitHub Repository Variables の設定手順
GitHub 側に、GCP に接続するための設定値を登録します。

**手順**:
1. GitHub で対象のリポジトリを開きます。
2. 上部の **Settings** タブを開きます。
3. 左側のメニューから **Secrets and variables** を展開し、**Actions** をクリックします。
4. **Variables** タブを選択します。
5. **New repository variable** ボタンを押して、以下の変数を一つずつ追加します。

| Name | Value の例 (プレースホルダーを実際の値に書き換えてください) |
| --- | --- |
| `GCP_PROJECT_ID` | `<YOUR_GCP_PROJECT_ID>` |
| `GCP_REGION` | `<YOUR_GCP_REGION>` |
| `GCP_WORKLOAD_IDENTITY_PROVIDER` | `<YOUR_WORKLOAD_IDENTITY_PROVIDER_RESOURCE_NAME>` |
| `GCP_DEPLOY_SERVICE_ACCOUNT` | `<YOUR_DEPLOY_SERVICE_ACCOUNT_EMAIL>` |
| `ARTIFACT_REPOSITORY` | `<YOUR_ARTIFACT_REGISTRY_REPOSITORY_NAME>` |
| `IMAGE_NAME` | `<YOUR_IMAGE_NAME>` |
| `GCS_BUCKET_NAME` | `<YOUR_GCS_BUCKET_NAME>` |
| `CLOUD_RUN_JOB_NAME` | `<YOUR_CLOUD_RUN_JOB_NAME>` |
| `BUILD_SERVICE_ACCOUNT_NAME` | `<YOUR_BUILD_SERVICE_ACCOUNT_NAME>` |
| `RUNTIME_SERVICE_ACCOUNT_NAME` | `<YOUR_RUNTIME_SERVICE_ACCOUNT_NAME>` |

> **`GCP_WORKLOAD_IDENTITY_PROVIDER` の値の確認方法**:
> 以下のコマンドを実行して表示される `name` の値（`projects/123456789/locations/global/...` のような長い文字列）をコピーして設定してください。
> ```bash
> gcloud iam workload-identity-pools providers list \
>   --project="$PROJECT_ID" \
>   --location="global" \
>   --workload-identity-pool="$WORKLOAD_IDENTITY_POOL" \
>   --format="value(name)"
> ```

**注意点**:
- API キーやパスワード、GMO API キー、API シークレットなどの秘密情報は **Variables** ではなく、必ず **Secrets** タブの方に登録してください。
- Public リポジトリであっても、Repository Variables の設定画面上の値は一般ユーザーには見えません。
- ただし、workflow の中で `echo ${{ vars.XXX }}` のように出力すると、Actions のログから値が見えてしまう可能性があるため、不要なログ出力は避けてください。

---

## 最終確認コマンド
全ての設定が正しく行われているか、以下のコマンドをまとめて実行して確認します。

```bash
echo "=== project ==="
gcloud projects describe "$PROJECT_ID" \
  --format="table(projectId,projectNumber,lifecycleState)"

echo "=== billing ==="
gcloud billing projects describe "$PROJECT_ID" \
  --format="table(billingAccountName,billingEnabled)"

echo "=== enabled services ==="
gcloud services list \
  --enabled \
  --project "$PROJECT_ID" \
  --filter="NAME:(serviceusage.googleapis.com OR iam.googleapis.com OR iamcredentials.googleapis.com OR cloudbuild.googleapis.com OR artifactregistry.googleapis.com OR run.googleapis.com OR storage.googleapis.com)" \
  --format="table(NAME,TITLE)"

echo "=== deploy service account ==="
gcloud iam service-accounts describe "$DEPLOY_SERVICE_ACCOUNT_EMAIL" \
  --project "$PROJECT_ID" \
  --format="table(email,displayName,disabled)"

echo "=== deploy service account roles ==="
gcloud projects get-iam-policy "$PROJECT_ID" \
  --flatten="bindings[].members" \
  --filter="bindings.members:serviceAccount:${DEPLOY_SERVICE_ACCOUNT_EMAIL}" \
  --format="table(bindings.role)"

echo "=== required role check ==="

REQUIRED_ROLES=(
  "roles/serviceusage.serviceUsageAdmin"
  "roles/artifactregistry.admin"
  "roles/storage.admin"
  "roles/iam.serviceAccountAdmin"
  "roles/resourcemanager.projectIamAdmin"
  "roles/cloudbuild.builds.editor"
  "roles/run.admin"
  "roles/iam.serviceAccountUser"
)

ACTUAL_ROLES="$(gcloud projects get-iam-policy "$PROJECT_ID" \
  --flatten="bindings[].members" \
  --filter="bindings.members:serviceAccount:${DEPLOY_SERVICE_ACCOUNT_EMAIL}" \
  --format="value(bindings.role)")"

for ROLE in "${REQUIRED_ROLES[@]}"; do
  if echo "$ACTUAL_ROLES" | grep -qx "$ROLE"; then
    echo "OK      $ROLE"
  else
    echo "MISSING $ROLE"
  fi
done

echo "=== workload identity provider ==="
gcloud iam workload-identity-pools providers list \
  --project="$PROJECT_ID" \
  --location="global" \
  --workload-identity-pool="$WORKLOAD_IDENTITY_POOL" \
  --format="table(name,displayName,state)"

echo "=== workload identity binding ==="
gcloud iam service-accounts get-iam-policy "$DEPLOY_SERVICE_ACCOUNT_EMAIL" \
  --project "$PROJECT_ID" \
  --format="table(bindings.role,bindings.members)"
```

**最終的にどうなればよいか（チェックリスト）**:
- [ ] GCP プロジェクトが `ACTIVE`
- [ ] 課金が有効 (`billingEnabled` が `True`)
- [ ] 必要 API が有効
- [ ] デプロイ用サービスアカウントが存在する
- [ ] required role check がすべて `OK` になっている
- [ ] Workload Identity Federation の Provider が存在する
- [ ] デプロイ用サービスアカウントに `roles/iam.workloadIdentityUser` が付与されている
- [ ] GitHub Repository Variables が全て登録されている

---

## GitHub Actions の実行確認手順
以上の設定が終わったら、実際にデプロイを試してみましょう。

1. GitHub リポジトリの **Actions** タブを開きます。
2. 左側の workflow 一覧から **Deploy to GCP** などの対象 workflow を選びます。
3. **Run workflow** ボタンを押して実行します。
4. 実行ログを開き、以下のステップが成功しているか確認します。
   - GCP への認証 (Authenticate to Google Cloud)
   - Cloud Build でのビルド
   - Cloud Run Job へのデプロイ

必要に応じて、デプロイが成功したかを gcloud コマンドでも確認できます。
```bash
gcloud run jobs describe "$CLOUD_RUN_JOB_NAME" \
  --region "$REGION" \
  --project "$PROJECT_ID"
```

---

## トラブルシューティング
よくあるエラーとその対処方法です。

### `gh: command not found`
GitHub CLI がインストールされていない場合に発生します。このドキュメントの手順はブラウザ（GitHub 画面）から Variables を設定するため、無視して構いません。

### `PERMISSION_DENIED`
デプロイ用サービスアカウントの IAM ロールが不足しています。「5. デプロイ用サービスアカウントへの IAM 付与手順」を再度確認してください。

### `NOT_FOUND: Artifact Registry repository`
Artifact Registry が未作成である可能性があります。今後 workflow 側で自動作成されるようになれば、初回実行時に存在しなくても問題ありません。手動で作成する場合は Cloud Console から作成してください。

### `gs://... not found`
GCS バケットが未作成である可能性があります。こちらも今後 workflow 側で自動作成されるようになれば問題ありません。

### `workload_identity_provider` 関連のエラー
以下を確認してください。
- GitHub Variables の `GCP_WORKLOAD_IDENTITY_PROVIDER` が、Provider の `name` と完全に一致しているか。
- Workload Identity 作成時の `--attribute-condition` が対象の GitHub リポジトリ (`ht-0328/crypto-autotrading-lab` など) と一致しているか。

### `iam.serviceAccounts.actAs` 関連のエラー
デプロイ用サービスアカウントに `roles/iam.serviceAccountUser` が不足している可能性があります。

---

## 既存 workflow との関係
現在のリポジトリにあるデプロイ関連の設定ファイルは以下の役割を持っています。

- **`.github/workflows/deploy-gcp.yml`**
  - GitHub Actions で GCP に認証する。
  - Cloud Build を実行する。
  - Cloud Run Job にデプロイする。
- **`cloudbuild.yaml`**
  - Gradle で shadowJar を作成する。
  - Docker イメージをビルドする。
  - Artifact Registry にプッシュする。

**今後の改善について**:
- `deploy-gcp.yml` に setup 用の job を追加すると、Artifact Registry、GCS、サービスアカウント作成、IAM 付与を全て自動化できます。
- `cloudbuild.yaml` のイメージパス直書きを substitutions (変数) に置き換えることで、プロジェクト ID などの環境変更に強くなります。
