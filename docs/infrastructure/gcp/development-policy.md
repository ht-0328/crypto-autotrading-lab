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
