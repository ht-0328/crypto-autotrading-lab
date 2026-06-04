# GCP インフラコード設計書

このドキュメントは、TerraformなどでGCPインフラをコード化するための設計書です。

## 1. 設計の目的

- GCP上に必要なリソースをコードで再現できるようにする
- 手作業やGitHub Actions内の `gcloud` コマンドに依存しすぎない構成にする
- IAM権限、サービスアカウント、保存先、デプロイ先の関係を設計として明確にする
- アプリの実装ではなく、GCPインフラの構成を定義する

## 2. 対象範囲

インフラコードで管理する対象と、対象外の項目を以下に定義します。

### 対象

- GCP API 有効化
- Artifact Registry
- GCS Bucket
- Cloud Build 用 Service Account
- Cloud Run Runtime 用 Service Account
- IAM binding
- Cloud Run Job のインフラ定義
- 将来的な Cloud Scheduler

### 対象外

- Kotlinアプリ本体
- 売買ロジック
- Docker image の build 処理
- Cloud Run Job の手動実行
- 実注文処理
- GitHub Actions のデプロイ実行手順

## 3. 全体構成

本システムのGCPインフラは、GitHub Actionsからデプロイを行い、Cloud Run Jobとして実行される構成となっています。
権限とリソースの関係は以下の通りです。

```text
GitHub Actions
  |
  | Workload Identity Federation
  v
Deploy Service Account
  |
  +-- Cloud Build Service Account
  |     |
  |     +-- Artifact Registry へ Docker image を push
  |
  +-- Cloud Run Runtime Service Account
        |
        +-- Cloud Run Job を実行
        |
        +-- GCS Bucket に結果・状態ファイルを読み書き
```

## 4. リソース設計

| リソース | 用途 | 管理方針 |
|---|---|---|
| GCP API | 必要なGCPサービスを利用可能にする | インフラコードで管理 |
| Artifact Registry | Docker image の保存先 | インフラコードで管理 |
| GCS Bucket | シミュレーション結果・状態ファイルの保存先 | インフラコードで管理 |
| Cloud Build Service Account | Docker image の build / push 実行主体 | インフラコードで管理 |
| Cloud Run Runtime Service Account | Cloud Run Job の実行主体 | インフラコードで管理 |
| IAM binding | 各Service Accountに必要な権限を付与 | インフラコードで管理 |
| Cloud Run Job | アプリを実行するジョブ定義 | 将来的にインフラコードで管理 |
| Cloud Scheduler | 定期実行の起点 | 将来的にインフラコードで管理 |

## 5. Terraform ファイル構成案

- infra/
  - terraform/
    - gcp/
      - versions.tf
      - providers.tf
      - variables.tf
      - apis.tf
      - artifact-registry.tf
      - storage.tf
      - service-accounts.tf
      - iam.tf
      - cloud-run-job.tf
      - outputs.tf
      - terraform.tfvars.example

| ファイル | 役割 |
|---|---|
| versions.tf | Terraform本体とGoogle providerのバージョンを固定する |
| providers.tf | Google Cloud provider の設定を書く |
| variables.tf | project_id, region など外から渡す値を定義する |
| apis.tf | 有効化するGCP APIを定義する |
| artifact-registry.tf | Artifact Registry を定義する |
| storage.tf | GCS Bucket を定義する |
| service-accounts.tf | Service Account を定義する |
| iam.tf | IAM binding を定義する |
| cloud-run-job.tf | Cloud Run Job の定義を書く |
| outputs.tf | 作成したリソース名などを出力する |
| terraform.tfvars.example | 設定値の例を書く |

## 6. 変数設計

| 変数名 | 意味 | 例 |
|---|---|---|
| project_id | GCPプロジェクトID | crypto-autotrading-lab |
| region | GCPリージョン | asia-northeast1 |
| artifact_repository_name | Artifact Registry名 | crypto-autotrading-lab |
| gcs_bucket_name | GCS Bucket名 | crypto-autotrading-lab |
| build_service_account_name | Cloud Build用SA名 | cloud-build-builder |
| runtime_service_account_name | Cloud Run実行用SA名 | crypto-autotrading-lab-runner |
| deploy_service_account_email | GitHub Actions用SAメール | github-actions-deployer@crypto-autotrading-lab.iam.gserviceaccount.com |
| cloud_run_job_name | Cloud Run Job名 | 既存のGitHub Actions varsに合わせる |

※ SA は Service Account の略です。
Service Account は、GCP上でプログラムやGitHub Actionsが操作するときに使う専用アカウントです。

## 7. IAM設計

| 付与先 | 権限 | 目的 |
|---|---|---|
| Cloud Build Service Account | roles/artifactregistry.writer | Docker image を Artifact Registry にpushするため |
| Cloud Build Service Account | roles/logging.logWriter | buildログを書き込むため |
| Cloud Build Service Account | roles/storage.objectViewer | build時に必要なオブジェクトを読むため |
| Cloud Run Runtime Service Account | roles/storage.objectAdmin on GCS Bucket | 実行結果や状態ファイルを読み書きするため |
| Deploy Service Account | roles/iam.serviceAccountUser on Cloud Build SA | Cloud Build用SAを指定してbuildするため |
| Deploy Service Account | roles/iam.serviceAccountUser on Runtime SA | Cloud Run Jobに実行用SAを指定するため |

**禁止事項:**
- `google_project_iam_policy` は使わない
  - 理由: プロジェクト全体のIAMを上書きして、既存権限を壊す危険があるため
- IAMは以下の個別付与で管理する
  - `google_project_iam_member`
  - `google_storage_bucket_iam_member`
  - `google_service_account_iam_member`

## 8. state設計

- stateとは、Terraformが「どのGCPリソースを管理しているか」を記録する情報
- state はローカルPCだけに置かない
- GCS Bucket を backend として使う想定
- state の保存先 prefix は `terraform/gcp` のようにする
- 今回のPRでは backend の実装コードは追加しない

## 9. 既存リソース取り込み設計

- importとは、すでにGCP上に存在するリソースをTerraform管理へ取り込む作業
- 取り込み対象:
  - Artifact Registry
  - GCS Bucket
  - Cloud Build Service Account
  - Cloud Run Runtime Service Account
  - 必要な IAM binding
- importせずに `terraform apply` すると、同じ名前のリソースを新規作成しようとして失敗する可能性がある

## 10. GitHub Actionsとの責務分離

| 対象 | Terraform側 | GitHub Actions側 |
|---|---|---|
| GCP API有効化 | 管理する | 原則やらない |
| Artifact Registry作成 | 管理する | 原則やらない |
| GCS Bucket作成 | 管理する | 原則やらない |
| Service Account作成 | 管理する | 原則やらない |
| IAM付与 | 管理する | 原則やらない |
| Docker image build | 管理しない | 実行する |
| Docker image push | 管理しない | 実行する |
| Cloud Run Job deploy | 将来的に管理候補 | 当面実行する |
| Cloud Run Job execute | 管理しない | 実行する |

## 11. 命名設計

- 既存の GitHub Actions vars と名前を合わせる
- GCPリソース名はプロジェクト名を基準にする
- Service Account名は用途が分かる名前にする
- Terraform resource名はGCP上の表示名ではなく、コード上の役割が分かる名前にする

## 12. 設計上やってはいけないこと

- Terraform導入とアプリ改修を同じPRに混ぜない
- IAM全体を上書きしない
- 既存のbootstrap/deploy workflowをいきなり削除しない
- Phase1のシミュレーション運用前提を壊さない
- 実注文前提のインフラ設計に変えない
- SecretやAPIキーをTerraformコードに直接書かない
- `terraform.tfstate` をGit管理しない
