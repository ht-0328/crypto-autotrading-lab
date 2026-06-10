# GCP インフラストラクチャ構成 (Terraform)

このディレクトリには、GCPインフラリソースをプロビジョニングし管理するためのTerraform構成が含まれています。

## Terraform と GitHub Actions の責務分離

このプロジェクトでは、TerraformとGitHub Actionsの間で責務を明確に分離しています：

- **Terraform (インフラ定義)**:
  - 必要なGCP APIの有効化
  - Artifact Registry リポジトリの作成
  - GCS Bucket の作成（アプリケーションデータ用およびTerraform state用）
  - Service Account の作成およびIAM権限の設定
  - Secret Manager シークレットのプロビジョニング（コンテナのみで、値は含まない）
  - Cloud Run Job と Cloud Scheduler ジョブの定義

- **GitHub Actions (アプリケーションのビルドと実行)**:
  - Dockerイメージのビルド
  - Artifact RegistryへのDockerイメージのプッシュ
  - Cloud Run Job の実行（例: `gcloud run jobs execute`）

*注: Cloud Run Jobの `image_uri` パラメータは、Terraform内で変数として提供されます。Terraformはイメージのビルドプロセスを管理せず、ジョブを正しく定義するために有効なイメージURIを期待します。*

## 既存リソース移行時の注意事項

現在は既存のGitHub Actionsによって、Artifact Registry、GCS Bucket、Service Accountなどのリソースがすでに作成されている場合があります。
そのため、既存環境に対してそのまま `terraform apply` を実行すると、既存リソースと衝突しエラーになる可能性があります。

**Terraform運用への移行手順（既存環境向け）**:
本番適用を行う前に、既存のリソースを `terraform import` コマンドを使用してTerraformのstateへ取り込む必要があります。
今回のPRではTerraformコードの追加のみを行っており、既存リソースのimportや本番適用は別のステップとして実施してください。

## Terraform ファイルの読み進め方

このディレクトリの Terraform 構成は、以下の順番で確認すると全体像を把握しやすくなります。

1. **`../bootstrap/gcp`** で Terraform の state 管理用 GCS Bucket を作成する
2. **`apis.tf`** でプロジェクトに必要な GCP API を有効化する
3. **`service-accounts.tf`** で各リソースが使用する Service Account を作成する
4. **`iam.tf`** で各 Service Account に必要な権限を付与する
5. **`storage.tf`**, **`artifact-registry.tf`**, **`secrets.tf`** でアプリケーションに必要な周辺リソースを作成する
6. **`cloud-run-job.tf`** でアプリケーション実行基盤となる Cloud Run Job を定義する
7. **`scheduler.tf`** で Cloud Run Job を定期実行するトリガーを定義する
