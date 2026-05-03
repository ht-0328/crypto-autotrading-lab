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
初回実行時は、必要な保管場所（Artifact Registry や GCS など）が自動で作られます。

## デプロイの3つのフェーズ

GCP へのデプロイ作業は、安全のために以下の3つのフェーズ（ワークフロー）に分かれています。

1. **Bootstrap (初期構築):** `Bootstrap GCP Resources`
   - 初回のみ実行します。APIの有効化やストレージ（Artifact Registry, GCS）、サービスアカウントの作成など、アプリの土台を作ります。
   - 作成直後は反映待ちの時間（数秒〜数十秒）がかかるため、再実行で成功する場合があります。
2. **Deploy (通常デプロイ):** `Deploy to GCP`
   - アプリの更新や設定変更のたびに実行します。初期構築で作られたリソースを使って、アプリ本体をデプロイします。
   - このフェーズでは新しいリソースを勝手に作成しません。強い権限（管理者権限など）を外して安全に実行できるようにしています。
3. **Cleanup (削除):** `Cleanup GCP Resources`
   - 不要になった時に実行します。デプロイしたアプリやデータを削除します。

> **注意:** 現在のフェーズ1（Phase1）はシミュレーション運用であり、実際の暗号資産の注文（実注文）は行いません。実注文を行う設定に変更しないでください。

## デプロイの手順

### Step 1: 初期構築 (Bootstrap)

1. GitHub リポジトリの **Actions** タブを開きます。
2. 左側の workflow 一覧から **Bootstrap GCP Resources** を選びます。
3. 右側の **Run workflow** ボタンを押して実行します。
4. 成功するまで待ちます。（エラーが出た場合はログを確認し、必要に応じて再実行してください）

### Step 2: 通常デプロイ (Deploy)

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
