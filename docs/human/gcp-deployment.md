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

## 3. リソースのクリーンアップ手順

検証や開発が終了し、GCP 上に作成したリソースを削除したい場合は、専用の GitHub Actions ワークフロー (`cleanup-gcp.yml`) を手動で実行します。

👉 **[リソースのクリーンアップ手順について (github-actions-gcp-deploy-setup.md 内に記載) ](./github-actions-gcp-deploy-setup.md#リソースのクリーンアップ手順-cleanup-gcp)**

---

## 4. Cloud Scheduler による定期実行の手順

Cloud Run Job をデプロイした後、指定したスケジュールで定期実行させたい場合は、専用の GitHub Actions ワークフロー (`scheduler-gcp.yml`) を手動で実行して Cloud Scheduler Job を作成します。
このワークフローでは、Scheduler の作成・更新・一時停止 (pause)・再開 (resume)・削除・手動実行 (run) を管理できます。

**実行方法**:
1. GitHub リポジトリの **Actions** タブを開きます。
2. 左側の workflow 一覧から **Cloud Scheduler Management** を選びます。
3. **Run workflow** を押し、プルダウンから目的の操作 (`create`, `pause`, `resume`, `delete`, `run`) を選択して実行します。
> ※削除 (`delete`) を行う場合のみ、`confirm_delete` 欄に `DELETE` と入力してください。

---

## 5. デプロイ時の取引戦略の選択 (strategy_name)

アプリケーションの取引戦略は、デプロイ時に動的に選択することができます。
これは `Strategy` パターンによって実装されており、GitHub Actions でのデプロイ時に以下の戦略を選択できます。

- `SafeReboundStrategy` (デフォルト): 買値を基準に利確・損切りを行う安全性を考慮した戦略。
- `SimpleContrarianStrategy`: 開発初期の単純な逆張りロジック（比較用）。

**戦略の渡し方と環境変数の確認:**
- Deploy to GCP ワークフロー (`deploy-gcp.yml`) を手動実行 (workflow_dispatch) する際、入力フォームから `strategy_name` を選択します。
- 選択された戦略は環境変数 `APP_TRADING_STRATEGY_NAME` として Cloud Run Job に設定されます。
- デプロイ後、GCP コンソールや CLI コマンド (`gcloud run jobs describe`) を使って環境変数を確認できます。
- **重要:** Cloud Scheduler は既存の Cloud Run Job を定期実行する役割のみを持ちます。したがって、実行される取引戦略は Cloud Run Job をデプロイした際の設定によって決まります。戦略を変更したい場合は、再度 `deploy-gcp.yml` ワークフローを実行して Cloud Run Job を更新してください。

---

## 6. 設定ファイルと環境変数の優先順位について (アーキテクチャの補足)

アプリケーションの設定値は、以下の優先順位で決定されます。これはローカル開発および Cloud Run などのデプロイ環境において共通のルールです：

1. **環境変数** (例: `APP_INTERVAL`, `TRADING_SYMBOL`)
2. **YAML設定ファイル** (例: `application-gmo.yaml`)
3. **アプリケーション側のデフォルト値**

この仕組みにより、以下の柔軟な運用が可能になっています：

* ローカル開発では、これまで通り YAML 設定ファイルを使用できます。
* Cloud Run Job のようなクラウド環境では、設定ファイル（`application-gmo.yaml` 等）を使わず、すべての設定値を GitHub Actions Variables 経由で環境変数として直接渡す方針を採用しています。
* これにより、環境ごとに設定ファイルを用意したり、イメージに組み込んだり・マウントしたりする手間を省き、GitHub の画面上から柔軟に設定値（例： `TRADING_SYMBOL` や `APP_INTERVAL` など）を変更してデプロイできます。
* 環境変数は、GitHub Actions の `.github/workflows/deploy-gcp.yml` 内で `gcloud run jobs deploy` の `--set-env-vars` を通じて Cloud Run Job に渡されます。データディレクトリのマウントパス (`APP_DATA_DIR=/mnt/gcs/data`) 等もここで設定されます。

> **Note:** API キーや API シークレットなどの秘密情報はこの優先順位の対象外です。GitHub Variables には設定せず、GitHub Secrets や Secret Manager などの安全な仕組みを利用して管理してください。
