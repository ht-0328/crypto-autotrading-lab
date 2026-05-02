# GCP デプロイ事前準備ガイド

本ドキュメントは、GitHub Actions を使用して Google Cloud Platform (GCP) の Cloud Run にアプリケーションをデプロイするための事前準備について説明します。
デプロイプロセスを効率化するため、GCP リソースの多くは GitHub Actions (`.github/workflows/deploy-gcp.yml`) によって自動で作成・設定されます。手動で行う必要があるのは、基盤となるプロジェクトや認証設定などに限られます。

## 1. 事前に必要な準備 (手動設定)

以下の項目は、GitHub Actions を実行する前に手動で設定する必要があります。

### 1.1 GCP プロジェクトと課金設定
- 紐づける GCP プロジェクト (`<YOUR_PROJECT_ID>`) を作成します。
- プロジェクトに対して有効な課金アカウントが設定されていることを確認してください。

### 1.2 GitHub Actions 用デプロイサービスアカウントの作成
GitHub Actions が GCP リソースの作成や操作（Cloud Build の実行、Cloud Run へのデプロイなど）を行うための権限を持つサービスアカウントです。
- **アカウント名例**: `<YOUR_DEPLOY_SA_NAME>@<YOUR_PROJECT_ID>.iam.gserviceaccount.com`
- **必要なロール**:
  - `roles/serviceusage.serviceUsageAdmin` (API の有効化)
  - `roles/artifactregistry.admin` (Artifact Registry リポジトリの作成)
  - `roles/storage.admin` (GCS バケットの作成)
  - `roles/iam.serviceAccountAdmin` (サービスアカウントの作成)
  - `roles/resourcemanager.projectIamAdmin` (IAM 権限の付与)
  - `roles/cloudbuild.builds.editor` (Cloud Build の実行)
  - `roles/run.admin` (Cloud Run へのデプロイ)
  - `roles/iam.serviceAccountUser` (他のサービスアカウントとして振る舞う権限)

### 1.3 Workload Identity Federation の設定
GitHub Actions から GCP へ安全に認証するため、サービスアカウントキー（JSON）ではなく Workload Identity を設定します。

1. **Workload Identity プールの作成**: 名前 `<YOUR_POOL_NAME>`
2. **Workload Identity プロバイダの作成**:
   - Issuer URL: `https://token.actions.githubusercontent.com`
   - 属性マッピング: `google.subject` = `assertion.sub`
3. **サービスアカウントのバインディング**:
   - 上記プロバイダから「デプロイ用サービスアカウント」へのアクセス（`roles/iam.workloadIdentityUser`）を許可します。

設定後、以下の形式の **Workload Identity プロバイダの完全なリソース名** を控えてください:
`projects/<YOUR_PROJECT_NUMBER>/locations/global/workloadIdentityPools/<YOUR_POOL_NAME>/providers/<YOUR_PROVIDER_NAME>`

### 1.4 GitHub Repository Variables の設定
GitHub のリポジトリ設定 (`Settings` > `Secrets and variables` > `Actions`) の **Variables** に、以下の環境変数を登録します。

| Variable 名 | 説明 | 値の例 |
| --- | --- | --- |
| `GCP_PROJECT_ID` | GCP プロジェクトID | `<YOUR_PROJECT_ID>` |
| `GCP_REGION` | リソースを作成するリージョン | `asia-northeast1` |
| `GCP_WORKLOAD_IDENTITY_PROVIDER` | Workload Identity プロバイダパス | `projects/<YOUR_PROJECT_NUMBER>/locations/global/...` |
| `GCP_DEPLOY_SERVICE_ACCOUNT` | デプロイ用サービスアカウントのメールアドレス | `<YOUR_DEPLOY_SA_NAME>@<YOUR_PROJECT_ID>.iam.gserviceaccount.com` |
| `ARTIFACT_REPOSITORY` | Artifact Registry リポジトリ名 | `crypto-autotrading-lab` |
| `IMAGE_NAME` | Docker イメージ名 | `crypto-autotrading-lab` |
| `GCS_BUCKET_NAME` | データの保存などに使う GCS バケット名 | `crypto-autotrading-lab-bucket` |
| `CLOUD_RUN_JOB_NAME` | Cloud Run Job の名前 | `crypto-autotrading-lab` |
| `BUILD_SERVICE_ACCOUNT_NAME` | Cloud Build 用に作成するSAの名前 (ID部分) | `cloud-build-builder` |
| `RUNTIME_SERVICE_ACCOUNT_NAME` | Cloud Run 実行用に作成するSAの名前 (ID部分) | `crypto-autotrading-runner` |

## 2. GitHub Actions が自動作成するもの

上記の事前準備が完了していれば、GitHub Actions 実行時に以下のリソースが自動的に作成・設定されます。（既に存在する場合は作成をスキップします）

- **必要な GCP API の有効化**
  - Cloud Build, Artifact Registry, Cloud Run, Storage, IAM, Service Usage, IAM Credentials API
- **Artifact Registry リポジトリの作成**
  - `${{ vars.ARTIFACT_REPOSITORY }}` として Docker 形式で作成されます。
- **Cloud Storage バケットの作成**
  - `${{ vars.GCS_BUCKET_NAME }}` として作成されます。
  - *注意: デプロイ後、設定ファイル (`application-gmo.yaml` 等) を手動でこのバケットの `config/` ディレクトリに配置する必要があります。*
- **Cloud Build ビルダー用サービスアカウントの作成と権限付与**
  - Cloud Build がイメージをビルドし、Artifact Registry にプッシュするためのアカウントが作成されます。
  - `roles/artifactregistry.writer`, `roles/logging.logWriter` が付与されます。
- **Cloud Run ランナー（実行用）サービスアカウントの作成と権限付与**
  - Cloud Run が実行時に GCS にアクセスするためのアカウントが作成されます。
  - 対象の GCS バケットに対する `roles/storage.objectAdmin` が付与されます。
- **Cloud Run Job のデプロイ**

※ これらは機密情報（Secret）として登録する必要はありませんが、必要に応じて Secrets を使用することも可能です。

## 6. 設定ファイルと環境変数の優先順位について

アプリケーションの設定値は、以下の優先順位で決定されます：

1. **環境変数** (例: `APP_INTERVAL`, `TRADING_SYMBOL`)
2. **YAML設定ファイル** (例: `application-gmo.yaml`)
3. **アプリケーション側のデフォルト値**

この仕組みにより、以下の柔軟な運用が可能です：

* ローカル開発では従来どおり YAML 設定ファイルを使用できます。
* Cloud Run Job では設定ファイルがなくても起動でき、本当に必要な設定値だけを環境変数で上書きできます。
* Cloud Run Job に渡す環境変数は、原則として `APP_DATA_DIR=/mnt/gcs/data` のみで動作します。

> **Note:** APIキーやAPIシークレットなどの秘密情報はこの優先順位の対象外です。将来的には Secret Manager などを使用して安全に管理する予定です。
