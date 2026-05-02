# GCP デプロイガイド

本ドキュメント群は、GitHub Actions を使用して Google Cloud Platform (GCP) にアプリケーションをデプロイするための手順を説明します。
プロセスを分かりやすくするため、手順を「事前準備」と「デプロイ設定」の 2 つのステップに分けています。初心者の方は、以下の順番でドキュメントを読み進めてください。

## 1. GCP アカウントとプロジェクトのセットアップ

GCP を初めて使う方向けの初期準備手順です。
Google アカウントの準備から、GCP プロジェクトの作成、課金設定、および `gcloud` CLI の導入と認証までを説明しています。

👉 **[GCP アカウントとプロジェクトのセットアップ (gcp-account-and-project-setup.md)](./gcp-account-and-project-setup.md)**

## 2. GitHub Actions から GCP にデプロイするための準備

GCP の準備が整った後、GitHub Actions から GCP リソースへ安全にアクセスし、デプロイを行うための設定手順です。
現在の `deploy-gcp.yml` では、GCP側の一部リソース（Artifact Registry、GCS、サービスアカウントなど）を GitHub Actions で自動作成する構成になっています。
ただし、GCPプロジェクト、課金設定、Workload Identity Federation、デプロイ用サービスアカウント、および GitHub Variables は事前準備として必要です。

👉 **[GitHub Actions から GCP にデプロイするための準備 (github-actions-gcp-deploy-setup.md)](./github-actions-gcp-deploy-setup.md)**

---

## 3. 設定ファイルと環境変数の優先順位について (アーキテクチャの補足)

アプリケーションの設定値は、以下の優先順位で決定されます。これはローカル開発および Cloud Run などのデプロイ環境において共通のルールです：

1. **環境変数** (例: `APP_INTERVAL`, `TRADING_SYMBOL`)
2. **YAML設定ファイル** (例: `application-gmo.yaml`)
3. **アプリケーション側のデフォルト値**

この仕組みにより、以下の柔軟な運用が可能になっています：

* ローカル開発では、これまで通り YAML 設定ファイルを使用できます。
* Cloud Run Job のようなクラウド環境では、設定ファイルがなくても起動でき、本当に必要な設定値だけを環境変数で上書きできます。
* Cloud Run Job に渡す環境変数は、原則として `APP_DATA_DIR=/mnt/gcs/data` (GCS のマウントパス) などのデータディレクトリの指定のみで基本動作するように設計されています。

> **Note:** API キーや API シークレットなどの秘密情報はこの優先順位の対象外です。GitHub Variables には設定せず、GitHub Secrets や Secret Manager などの安全な仕組みを利用して管理してください。
