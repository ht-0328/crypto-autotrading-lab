# 5. GitHub Actions の Variables 設定

## この文書で分かること
- GCP に接続するための情報を、GitHub 側に登録する方法

## 読む人
運用インフラ構築担当者

## 関連ドキュメント
- [06-deploy-cloud-run-job.md](06-deploy-cloud-run-job.md)

## まず結論
GitHub Actions が GCP にデプロイするためには、「どのプロジェクトの、どの入り口（Workload Identity）を使うか」を教えてあげる必要があります。
これらの情報を GitHub の **Repository Variables** に登録します。

## 1. 必要な情報を確認する
登録する前に、GCP側で払い出された正確な名前（Resource Name）を確認します。
ターミナルで以下のコマンドを実行し、表示された長い文字列（`projects/123456...`）をコピーしてメモしておいてください。

```bash
gcloud iam workload-identity-pools providers list \
  --project="$(gcloud config get-value project)" \
  --location="global" \
  --workload-identity-pool="github-actions-pool" \
  --format="value(name)"
```

## 2. GitHub に Variables を登録する
GitHub の画面から設定値を登録します。

1. GitHub で対象のリポジトリを開きます。
2. **Settings** タブを開きます。
3. 左側のメニューから **Secrets and variables** を展開し、**Actions** をクリックします。
4. **Variables** タブを選択します。
5. **New repository variable** ボタンを押して、以下の変数を一つずつ追加します。

| Name | 設定する値の例 |
| --- | --- |
| `GCP_PROJECT_ID` | `あなたのGCPプロジェクトID` |
| `GCP_REGION` | `asia-northeast1` (東京リージョンの場合) |
| `GCP_WORKLOAD_IDENTITY_PROVIDER` | `手順1でメモした長い文字列` |
| `GCP_DEPLOY_SERVICE_ACCOUNT` | `github-actions-deploy@あなたのGCPプロジェクトID.iam.gserviceaccount.com` |
| `ARTIFACT_REPOSITORY` | `crypto-app-repo` (好きな名前) |
| `IMAGE_NAME` | `crypto-autotrading-app` (好きな名前) |
| `GCS_BUCKET_NAME` | `crypto-data-bucket-あなたのGCPプロジェクトID` (世界で一意な名前) |
| `CLOUD_RUN_JOB_NAME` | `crypto-trading-job` (好きな名前) |
| `BUILD_SERVICE_ACCOUNT_NAME` | `crypto-build-sa` (※1) |
| `RUNTIME_SERVICE_ACCOUNT_NAME` | `crypto-runtime-sa` (※1) |
| `CLOUD_SCHEDULER_JOB_NAME` | `crypto-scheduler-job` (好きな名前) |
| `SCHEDULER_SERVICE_ACCOUNT_NAME` | `crypto-scheduler-sa` (※1) |
| `SCHEDULER_CRON` | `*/5 * * * *` (5分ごとの場合) |
| `SCHEDULER_TIME_ZONE` | `Asia/Tokyo` |
| `TRADING_SYMBOL` | `BTC` |

> **(※1) サービスアカウント名に関する注意**:
> `BUILD_SERVICE_ACCOUNT_NAME` などの値は、メールアドレスではなく「ID部分のみ（6〜30文字）」を指定してください。（例: `crypto-build-sa`）

> **取引戦略 (strategy_name) について**:
> 取引戦略はここでは設定しません。デプロイを実行する際（workflow_dispatch）の画面で選択します。

> **秘密情報について**:
> API キーやシークレットなどの秘密情報は、ここ（Variables）ではなく、隣の **Secrets** タブに登録してください。

## 完了条件チェックリスト
- [ ] 必要な設定値がすべて GitHub の Variables に登録された

終わったら、次は [06-deploy-cloud-run-job.md](06-deploy-cloud-run-job.md) に進んでください。
