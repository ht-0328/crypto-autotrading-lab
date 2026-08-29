# 8. リソースのクリーンアップ手順

## 文書の目的

- GCP に作ったアプリやデータを安全に削除（お掃除）する方法

## 対象読者

運用担当者

## 概要

GCP に作った Cloud Run Job や Dockerイメージ（Artifact Registry）は、置いておくだけでもお金がかかる場合があります。
検証が終わって不要になった場合は、専用の GitHub Actions ワークフロー **`Cleanup GCP Resources`** を使って安全に削除できます。

## 削除されるもの・残るもの

**自動で削除されるもの（この機能で消えるもの）:**

- Cloud Run Job
- Artifact Registry のリポジトリ（Dockerイメージ）
- 実行用・ビルド用のサービスアカウント
- （オプションを選んだ場合のみ）GCS バケットと保存されたファイル

**自動で削除されないもの（消えないもの）:**

- GCP プロジェクトそのもの
- 課金設定
- Workload Identity とデプロイ用サービスアカウント（※1）
- GitHub に登録した Variables や Secrets

!!! note "（※1）自動では消さないもの"

    （※1）これらを消してしまうと、次にまたデプロイしたくなった時に初めから設定し直す必要があり大変なため、自動では消さないようにしています。完全に消したい場合は、GCPの画面から手動で消してください。

## クリーンアップの手順

1. GitHub リポジトリの **Actions** タブを開きます。
2. 左側の workflow 一覧から **Cleanup GCP Resources** を選びます。
3. **Run workflow** ボタンを押します。
4. 以下の入力項目を設定します：
   - **confirm_delete** (必須): 誤操作を防ぐため、ここに `DELETE` と正確に入力します。
   - **delete_gcs_bucket**: CSVやJSONが保存されている GCS バケットも消す場合はチェックを入れます。
   - **dry_run**: ここにチェックを入れると「何を消す予定か」を確認するだけで、実際には消しません。（デフォルトはチェックありです）
5. まずは `dry_run` にチェックを入れたまま実行し、ログを見て問題ないか確認することを強くお勧めします。
6. 問題なければ、再度 `Run workflow` を開き、`dry_run` のチェックを外し、`confirm_delete` に `DELETE` を入力して実行します。

## クリーンアップ後の注意（再構築について）

クリーンアップ実行後、再度デプロイを行いたい場合は、以下の手順を**順番通り**に再実行する必要があります。

1. **Bootstrap Create GCP Resources**
2. **Bootstrap Grant IAM Permissions**
3. **Deploy to GCP**

### サービスアカウント削除に関する注意事項

通常のクリーンアップ運用では、サービスアカウントの削除は慎重に行ってください。
万が一 `cloud-build-builder` や `crypto-autotrading-lab-runner` などのサービスアカウントを削除した場合、GCPのIAM設定（ポリシーバインディング）に `deleted:serviceAccount:ユーザー名@...` という形で古い紐付けが残ることがあります。
この状態で再構築（Bootstrap Create）を行うと同名の新しいサービスアカウントが作られますが、内部IDが異なるため、古い `deleted:` のバインディングが邪魔をして「権限がない」とエラーになるなど、再構築時に混乱を招きやすくなります。

そのため、トラブルシューティング目的以外では、サービスアカウントの削除を避け、Cloud Run Job や Artifact Registry などのリソース削除に留める運用を推奨します。

## 完了条件チェックリスト

- [ ] Cleanup ワークフローが緑色（成功）で終わった
- [ ] GCP のコンソールで、Cloud Run Job や Artifact Registry が消えていることが確認できた
