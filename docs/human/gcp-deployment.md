# GCP デプロイ事前準備ガイド

本ドキュメントは、GitHub Actions を使用して Google Cloud Platform (GCP) の Cloud Run にアプリケーションをデプロイするための事前準備について説明します。
`.github/workflows/deploy-gcp.yml` および `cloudbuild.yaml` の動作に必要な API、リソース、IAM（サービスアカウント）、および GitHub Variables の設定を網羅しています。

## 1. 必要な GCP API の有効化

GCP プロジェクト (`<YOUR_PROJECT_ID>`) において、以下の API を有効化してください。

- **Cloud Build API** (`cloudbuild.googleapis.com`): コンテナイメージのビルドに使用
- **Artifact Registry API** (`artifactregistry.googleapis.com`): ビルドしたコンテナイメージの保存に使用
- **Cloud Run Admin API** (`run.googleapis.com`): アプリケーションのデプロイに使用
- **IAM Service Account Credentials API** (`iamcredentials.googleapis.com`): Workload Identity 連携による認証に使用

## 2. GCP リソースの作成

### 2.1 Artifact Registry リポジトリ

Docker イメージを保存するためのリポジトリを作成します。

- **形式**: Docker
- **ロケーション**: `<YOUR_REGION>` (例: `asia-northeast1`)
- **リポジトリ名**: `<YOUR_REPOSITORY_NAME>` (例: `crypto-autotrading-lab`)

*※イメージのパスは以下のようになります:*
`<YOUR_REGION>-docker.pkg.dev/<YOUR_PROJECT_ID>/<YOUR_REPOSITORY_NAME>/<YOUR_IMAGE_NAME>`

### 2.2 Cloud Storage バケット

アプリケーションが設定ファイル（`application-gmo.yaml` など）を読み込み、データ（`app.log` など）を出力するためのマウント用バケットを作成します。

- **バケット名**: `<YOUR_BUCKET_NAME>`
- **ロケーション**: `<YOUR_REGION>` またはマルチリージョン

バケット内に以下のディレクトリ/ファイル構成を準備してください:
- `<YOUR_BUCKET_NAME>/config/application-gmo.yaml`
- `<YOUR_BUCKET_NAME>/data/` (ログやその他の永続化データ用)

## 3. サービスアカウントと IAM 権限の設定

デプロイプロセスでは、役割ごとに3つのサービスアカウントを使用します。

### 3.1 デプロイ用サービスアカウント (GitHub Actions 連携用)
GitHub Actions が GCP でリソースを操作（Cloud Buildの起動、Cloud Runのデプロイ）するためのサービスアカウントです。
- **アカウント名例**: `<YOUR_DEPLOY_SA_NAME>@<YOUR_PROJECT_ID>.iam.gserviceaccount.com`
- **必要なロール**:
  - `roles/cloudbuild.builds.editor` (Cloud Build の実行)
  - `roles/run.admin` (Cloud Run へのデプロイ)
  - `roles/iam.serviceAccountUser` (Cloud Build ビルダー および Cloud Run ランナーのサービスアカウントとして振る舞う権限)
  - `roles/viewer` (基本的なリソースの確認)

### 3.2 Cloud Build ビルダー用サービスアカウント
Cloud Build がコンテナイメージをビルドし、Artifact Registry にプッシュするために使用します。
- **アカウント名例**: `<YOUR_BUILDER_SA_NAME>@<YOUR_PROJECT_ID>.iam.gserviceaccount.com`
- **必要なロール**:
  - `roles/artifactregistry.writer` (Artifact Registry へのイメージ書き込み)
  - `roles/logging.logWriter` (ビルドログの書き込み)
  - `roles/storage.objectAdmin` (Cloud Build で必要な一時的なストレージ操作)

### 3.3 Cloud Run ランナー（実行用）サービスアカウント
Cloud Run ジョブとして実行されるアプリケーション（コンテナ）が使用するサービスアカウントです。
- **アカウント名例**: `<YOUR_RUNNER_SA_NAME>@<YOUR_PROJECT_ID>.iam.gserviceaccount.com`
- **必要なロール**:
  - `roles/storage.objectAdmin` または `roles/storage.objectUser` (Cloud Storage バケット `<YOUR_BUCKET_NAME>` の読み書き、設定ファイルの読み込みおよびデータの書き込み)

## 4. Workload Identity Federation の設定

GitHub Actions から GCP へ安全に認証するため、サービスアカウントキー（JSON）ではなく Workload Identity を設定します。

1. **Workload Identity プールの作成**:
   - 名前: `<YOUR_POOL_NAME>`
2. **Workload Identity プロバイダの作成**:
   - プール内に OIDC プロバイダ (`<YOUR_PROVIDER_NAME>`) を作成します。
   - Issuer URL: `https://token.actions.githubusercontent.com`
   - 属性マッピング: `google.subject` = `assertion.sub`
   - 必要に応じて、特定のリポジトリのみ許可する属性条件を設定します (例: `assertion.repository == "your-org/your-repo"` )
3. **サービスアカウントのバインディング**:
   - プール/プロバイダから「3.1 デプロイ用サービスアカウント」へのアクセス（Workload Identity ユーザーロール `roles/iam.workloadIdentityUser`）を許可します。

設定後、以下の形式の **Workload Identity プロバイダの完全なリソース名** を控えてください:
`projects/<YOUR_PROJECT_NUMBER>/locations/global/workloadIdentityPools/<YOUR_POOL_NAME>/providers/<YOUR_PROVIDER_NAME>`

## 5. GitHub Variables の設定

GitHub のリポジトリ設定 (`Settings` > `Secrets and variables` > `Actions`) の **Variables** に、以下の環境変数を登録します。

| Variable 名 | 説明 | 値の例 |
| --- | --- | --- |
| `GCP_PROJECT_ID` | GCP のプロジェクトID | `<YOUR_PROJECT_ID>` |
| `GCP_WORKLOAD_IDENTITY_PROVIDER` | Workload Identity プロバイダの完全なパス | `projects/<YOUR_PROJECT_NUMBER>/locations/global/workloadIdentityPools/<YOUR_POOL_NAME>/providers/<YOUR_PROVIDER_NAME>` |
| `GCP_DEPLOY_SERVICE_ACCOUNT` | 3.1 で作成したデプロイ用サービスアカウントのメールアドレス | `<YOUR_DEPLOY_SA_NAME>@<YOUR_PROJECT_ID>.iam.gserviceaccount.com` |

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
