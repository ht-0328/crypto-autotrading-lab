# 6. Cloud Run Job へのデプロイ

## 文書の目的

- GitHub Actions を使って GCP にアプリをデプロイする手順
- デプロイ時の取引戦略の選び方

## 対象読者

運用担当者、開発メンバー

## 関連ドキュメント

- [07-scheduler.md](07-scheduler.md)

## 概要

設定がすべて終わったら、GitHub Actions の画面からボタンを押すだけで、GCP の Cloud Run Job にアプリをデプロイできます。
初回は `bootstrap-create-gcp.yml` と `bootstrap-grant-iam.yml` を順番に実行します。
通常デプロイでは、既に作成済み・権限設定済みのリソースを使います。
Artifact Registry や GCS などは `deploy-gcp.yml` では自動作成しません。

## GCP 運用手順（3段階）

GCP へのデプロイ作業は、安全のために以下の3段階のワークフローに分かれています。

1. **GCP の箱を作る (初期構築):** `bootstrap-create-gcp.yml`
   - 初回のみ実行します。APIの有効化やストレージ（Artifact Registry, GCS）、サービスアカウントの作成など、アプリの土台となる「箱」を作ります。
2. **作成済みの箱に権限を付ける (IAM付与):** `bootstrap-grant-iam.yml`
   - 初期構築で作ったリソースに対して、必要な権限（IAMロール）を付与します。
   - 分離することで、リソース作成直後の反映待ちによるエラーを防ぎます。
3. **作成済み・権限設定済みの環境へアプリを反映する (通常デプロイ):** `deploy-gcp.yml`
   - アプリの更新や設定変更のたびに実行します。このフェーズでは新しいリソースを勝手に作成せず、強い権限を外して安全に実行できるようにしています。
4. **Cleanup (削除):** `cleanup-gcp.yml`
   - 不要になった時に実行します。デプロイしたアプリやデータを削除します。

> **注意:** 現在のフェーズ1（Phase1）はシミュレーション運用であり、実際の暗号資産の注文（実注文）は行いません。実注文を行う設定に変更しないでください。

## デプロイの手順

### Step 1: 初期構築リソース作成

1. GitHub リポジトリの **Actions** タブを開きます。
2. 左側の workflow 一覧から **Bootstrap Create GCP Resources** を選びます。
3. 右側の **Run workflow** ボタンを押して実行し、成功するまで待ちます。

### Step 2: 初期構築IAM権限付与

1. 左側の workflow 一覧から **Bootstrap Grant IAM Permissions** を選びます。
2. 右側の **Run workflow** ボタンを押して実行し、成功するまで待ちます。

### Step 3: 通常デプロイ

1. 左側の workflow 一覧から **Deploy to GCP** を選びます。
2. 右側の **Run workflow** ボタンを押します。
3. 実行時の設定フォームが表示されます。
   - `execute_after_deploy`:
     - **チェックを入れる (true)**: デプロイ完了後、すぐに Cloud Run Job を1回動かします。
     - **チェックを外す (false - デフォルト)**: デプロイだけ行い、アプリは動かしません。
   - `strategy_name`:
     - 利用する取引戦略を選択します。（デフォルトは `SafeReboundStrategy`）
4. 緑色の **Run workflow** ボタンを押して実行します。

## 実行ログの確認

ログを開いて、以下のステップが成功しているか確認してください。

- **Authenticate to Google Cloud**: GCPに正しく接続できたか。
- **Verify GCP Resources Exist**: 初期構築で作られたリソースが正しく認識されているか。
- **Build and push Docker image**: プログラムがビルドされて保存されたか。
- **Deploy to Cloud Run Job**: Cloud Run Job に設定が反映されたか。

## デプロイ後の確認（任意）

ターミナルから以下のコマンドを打つと、設定された環境変数などを確認できます。

```bash
gcloud run jobs describe "<YOUR_CLOUD_RUN_JOB_NAME>" \
  --region "<YOUR_GCP_REGION>" \
  --project "<YOUR_GCP_PROJECT_ID>"
```

## 完了条件チェックリスト

- [ ] GitHub Actions の Deploy to GCP が緑色（成功）で終わった
- [ ] GCP のコンソールで Cloud Run Job が作られていることが確認できた

終わったら、次は [07-scheduler.md](07-scheduler.md) に進んで、定期実行の設定を行います。
