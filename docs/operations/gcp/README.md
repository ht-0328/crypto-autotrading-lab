# GCP 運用・デプロイガイド

このディレクトリには、GitHub Actions から GCP へシステムをデプロイし、運用するための手順書が順番に格納されています。
初めて構築する場合は、番号順に読み進めてください。

## 手順書一覧

- [01-account-and-project.md](01-account-and-project.md): GCPアカウントとプロジェクトの準備
- [02-gcloud-cli.md](02-gcloud-cli.md): gcloud CLI の準備とログイン
- [03-workload-identity-federation.md](03-workload-identity-federation.md): GitHub Actions との安全な連携設定
- [04-service-accounts-and-iam.md](04-service-accounts-and-iam.md): デプロイ用アカウントの作成と権限設定
- [05-github-actions-variables.md](05-github-actions-variables.md): GitHub への設定値登録
- [06-deploy-cloud-run-job.md](06-deploy-cloud-run-job.md): Cloud Run Job へのデプロイ実行
- [07-scheduler.md](07-scheduler.md): Cloud Scheduler による定期実行設定
- [08-cleanup.md](08-cleanup.md): （不要になった場合の）リソース削除手順
