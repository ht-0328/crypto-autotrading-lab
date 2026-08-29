# GCP 運用・デプロイガイド

| 項目 | 内容 |
| --- | --- |
| 想定読者 | GCP へ初めてデプロイする人 |
| 読んだあとできること | 8つの手順のどこから始め、どこまで進んだかを把握できる |
| 状態 | 現行 |
| 機密区分 | 公開可 |
| 作成者 | リポジトリ管理者 |
| 保守責任者 | リポジトリ管理者 |
| 最終確認日 | 2026-08-30 |


このディレクトリには、GCP へのデプロイと運用の手順書が順番に入っています。
初めて構築する場合は、番号順に読み進めてください。

## 手順書一覧

01 から順に実施します。08 は不要になった場合だけ実施します。

| 手順 | 内容 |
| --- | --- |
| [01 アカウントとプロジェクト](01-account-and-project.md) | GCPアカウントとプロジェクトの準備 |
| [02 gcloud CLI](02-gcloud-cli.md) | gcloud CLI の準備とログイン |
| [03 Workload Identity Federation](03-workload-identity-federation.md) | GitHub Actions との安全な連携設定 |
| [04 サービスアカウントとIAM](04-service-accounts-and-iam.md) | デプロイ用アカウントの作成と権限設定 |
| [05 GitHub Actions Variables](05-github-actions-variables.md) | GitHub への設定値登録 |
| [06 Cloud Run Job へのデプロイ](06-deploy-cloud-run-job.md) | デプロイの実行 |
| [07 Cloud Scheduler](07-scheduler.md) | 定期実行の設定 |
| [08 クリーンアップ](08-cleanup.md) | 不要になった場合のリソース削除 |
