# 6. Cloud Run Job へのデプロイ

| 項目 | 内容 |
| --- | --- |
| 想定読者 | アプリを GCP へデプロイする運用担当者 |
| 読んだあとできること | GitHub Actions から Cloud Run Job へデプロイできる |
| 状態 | 現行 |
| 機密区分 | 公開可 |
| 作成者 | リポジトリ管理者 |
| 保守責任者 | リポジトリ管理者 |
| 最終確認日 | 2026-08-30 |


## 文書の目的

- GitHub Actions を使って GCP にアプリをデプロイする手順
- デプロイ時の取引戦略の選び方

## 対象読者

運用担当者、開発メンバー

## 関連ドキュメント

- [07-scheduler.md](07-scheduler.md)

## 概要

設定が終わったら、GitHub Actions の画面からボタンを押すだけでデプロイできます。
初回は `bootstrap-create-gcp.yml` と `bootstrap-grant-iam.yml` を順番に実行します。
通常デプロイでは、既に作成済み・権限設定済みのリソースを使います。
Artifact Registry や GCS などは `deploy-gcp.yml` では自動作成しません。

## GCP 初期構築と通常デプロイの実行順

GCP へのデプロイ作業は、安全のために以下の3段階のワークフローに分かれています。
必ず以下の順番で実行してください。

1. **Bootstrap Create GCP Resources (`bootstrap-create-gcp.yml`)**
   - 初回（またはクリーンアップ後）のみ実行します。
   - 役割: GCP API の有効化と、リソースの「箱」を作ります。
2. **Bootstrap Grant IAM Permissions (`bootstrap-grant-iam.yml`)**
   - リソース作成後（ステップ1の後）に実行します。
   - 役割: 作成済みのリソースに対して、必要なIAM権限を付与します。リソース作成直後の反映待ちによるエラーを防ぐため、ステップ1と分離されています。
3. **Deploy to GCP (`deploy-gcp.yml`)**
   - アプリの更新や設定変更のたびに実行します。
   - 役割: 作成済みのリソースを使って、Cloud Run Job にアプリを反映します。このフェーズでは新しいリソースを作成しません。

（不要になった場合は `cleanup-gcp.yml` でリソースを削除します）

!!! warning "Phase1 の制約"

    このプロジェクトは Phase1 です。実際の注文は行わないシミュレーション運用です。実注文を行うような設定・説明には変更しないでください。

## デプロイの手順

初期構築、権限付与、デプロイの順に GitHub Actions を実行します。

### Step 1: 初期構築リソース作成

1. GitHub リポジトリの **Actions** タブを開きます。
2. **Bootstrap Create GCP Resources** を選びます。
3. 右側の **Run workflow** ボタンを押して実行し、成功するまで待ちます。

### Step 2: 初期構築IAM権限付与

1. **Bootstrap Grant IAM Permissions** を選びます。
2. 右側の **Run workflow** ボタンを押して実行し、成功するまで待ちます。

### Step 3: 通常デプロイ

1. 左側の workflow 一覧から **Deploy to GCP** を選びます。
2. 右側の **Run workflow** ボタンを押します。
3. 実行時の設定フォームが表示されます。
   - `execute_after_deploy`:
     - **true**: デプロイ完了後、すぐに Cloud Run Job を1回動かします。
     - **チェックを外す (false - デフォルト)**: デプロイだけ行い、アプリは動かしません。
   - `strategy_name`:
     - 利用する取引戦略を選択します。（デフォルトは `SafeReboundStrategy`）
4. 緑色の **Run workflow** ボタンを押して実行します。

## 実行ログの確認

ログを開いて、以下のステップが成功しているか確認してください。

- **Authenticate to Google Cloud**: GCPに正しく接続できたか。
- **Verify GCP Resources Exist**: リソースを認識できているか。
- **Build and push Docker image**: ビルドと保存ができたか。
- **Deploy to Cloud Run Job**: 設定が反映されたか。

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
