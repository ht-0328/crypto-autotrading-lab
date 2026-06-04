# GCP インフラ開発方針

## 文書の目的
- GCP インフラをどの方針で開発・管理するかを整理する
- 既存 GitHub Actions と、将来追加する Terraform の役割分担を明確にする
- どの作業を Terraform 化し、どの作業を GitHub Actions に残すかを判断できるようにする

## 対象読者
- インフラ開発担当者
- GitHub Actions / GCP / Terraform の修正を行う開発メンバー
- PRレビュー担当者

## 現在の構成
現在の GCP 関連 workflow は、以下の役割に分かれていることを説明します。

- `bootstrap-create-gcp.yml`
  - GCP API の有効化
  - Artifact Registry 作成
  - GCS Bucket 作成
  - Cloud Build 用 Service Account 作成
  - Cloud Run 実行用 Service Account 作成

- `bootstrap-grant-iam.yml`
  - Cloud Build 用 Service Account への権限付与
  - Cloud Run 実行用 Service Account への権限付与
  - GitHub Actions 用 Service Account への `roles/iam.serviceAccountUser` 付与

- `deploy-gcp.yml`
  - 既存リソースの存在確認
  - Cloud Build による Docker image build / push
  - Cloud Run Job への deploy
  - 必要に応じた Cloud Run Job の手動実行

## インフラ開発で管理するもの

| 分類 | 例 | 管理方針 |
|---|---|---|
| GCP の土台リソース | Artifact Registry, GCS Bucket, Service Account | 将来的に Terraform 管理へ寄せる |
| IAM 権限 | project IAM, bucket IAM, service account IAM | 将来的に Terraform 管理へ寄せる |
| アプリのビルド | Cloud Build による Docker image build | GitHub Actions に残す |
| アプリのデプロイ | Cloud Run Job deploy | 当面は GitHub Actions に残す |
| ジョブの実行 | Cloud Run Job execute | GitHub Actions に残す |
| 定期実行 | Cloud Scheduler | 将来的に Terraform 管理を検討する |

## Terraform 化する理由
- Terraform は、クラウドの設定をコードで管理するための道具である
- `gcloud` コマンドを workflow に直接書き続けると、YAML が手順書のように肥大化する
- Terraform に寄せると、どの GCP リソースと IAM 権限を管理しているかがファイルで見える
- PRレビューで「何のインフラ変更か」を確認しやすくなる
- 同じ構成を再現しやすくなる

## 既存 GitHub Actions を残す理由
- 既存 workflow はすでに動いているため、すぐ削除しない
- Terraform 導入直後は既存リソースの import が必要になる
- import が完了するまでは、既存 bootstrap workflow を初期構築・復旧用として残す
- `deploy-gcp.yml` はアプリのビルド・デプロイ用なので、Terraform 導入後も残す

## Terraform 導入時の基本方針
- 最初から全てを Terraform 化しない
- まずは GCP の土台リソースと IAM 権限を Terraform 化する
- 既存 workflow は削除せず、Terraform と並行して運用できる状態にする
- Terraform の `plan` で差分を確認してから `apply` する
- 既存リソースを Terraform 管理に入れる場合は `terraform import` を使う
- `google_project_iam_policy` のように IAM 全体を上書きするリソースは使わない
- IAM は `google_project_iam_member`、`google_storage_bucket_iam_member`、`google_service_account_iam_member` のように、個別付与の形で管理する

## 開発時の判断基準

| やりたい変更 | Terraform 向きか | 理由 |
|---|---|---|
| Artifact Registry を追加する | はい | 固定のインフラリソースだから |
| GCS Bucket を追加する | はい | 名前・場所・権限を管理したいから |
| Service Account を追加する | はい | 誰が何を実行するかを明確にしたいから |
| IAM 権限を追加する | はい | 権限の付与先と理由をレビューしたいから |
| Docker image を build する | いいえ | 毎回変わる成果物なので GitHub Actions 向き |
| Cloud Run Job を今すぐ実行する | いいえ | 一回限りの実行操作なので GitHub Actions 向き |
| アプリの取引戦略を選んでデプロイする | いいえ | 実行時の指定なので GitHub Actions 向き |

## PRレビュー観点
- [ ] 既存 workflow を削除していない
- [ ] `deploy-gcp.yml` の役割を Terraform に無理に移していない
- [ ] GCP リソース作成とアプリデプロイの責務が混ざっていない
- [ ] IAM 権限を広く付けすぎていない
- [ ] Terraform 化する対象としない対象が説明されている
- [ ] 既存リソースを扱う場合、import の必要性が説明されている
- [ ] Phase1 のシミュレーション運用前提を壊していない

## 今後の進め方
1. インフラ開発方針ドキュメントを追加する
2. Terraform の最小構成を追加する
3. Terraform plan 用 workflow を追加する
4. 既存 GCP リソースを import する
5. Terraform plan で差分が妥当か確認する
6. 手動実行の Terraform apply workflow を追加・検証する
7. 安定後に bootstrap workflow の扱いを再判断する

## 関連ドキュメント
- `docs/operations/gcp/04-service-accounts-and-iam.md`
- `docs/operations/gcp/05-github-actions-variables.md`
- `docs/operations/gcp/06-deploy-cloud-run-job.md`
- `docs/operations/gcp/07-scheduler.md`
- `docs/operations/gcp/08-cleanup.md`
