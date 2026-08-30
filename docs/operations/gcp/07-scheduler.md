# 7. Cloud Scheduler による定期実行の設定

## 文書の目的

- デプロイした Cloud Run Job を定期的に（例：5分ごと）動かす方法

## 対象読者

運用担当者

## 関連ドキュメント

- [06-deploy-cloud-run-job.md](06-deploy-cloud-run-job.md)
- [09-status-check.md](09-status-check.md)

## 概要

Cloud Run Job は「1回だけ動いて終わる」仕組みです。自動売買システムとして動かし続けるには、**Cloud Scheduler** を使って定期的に（タイマーで）呼び出す必要があります。
これも GitHub Actions から設定できます。

## このワークフローが作るもの

`create` を選んだとき、**Cloud Scheduler Management** は3つのことを行います。定期実行のタイマーを作るだけではありません。

| 作るもの | 役割 |
| --- | --- |
| スケジューラ用サービスアカウント（`SCHEDULER_SERVICE_ACCOUNT_NAME`） | Cloud Scheduler が Cloud Run Job を起動するときの実行主体 |
| そのサービスアカウントへの `roles/run.invoker` | これが無いと、起動要求は 403 で失敗する |
| Cloud Scheduler のジョブ | 決まった間隔で Cloud Run Job を起動するタイマー |

!!! warning "GCP 環境を作り直したら、必ずこのワークフローを実行してください"

    スケジューラ用サービスアカウントを作るのは、このワークフローだけです。[bootstrap-create-gcp.yml](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/.github/workflows/bootstrap-create-gcp.yml) が作るのは、ビルド用と実行用のサービスアカウントだけです。

    そのため、環境を作り直してデプロイまで終えても、このワークフローを実行しなければ**タイマーだけが残って起動に失敗し続ける**状態になります。Cloud Scheduler の状態は `ENABLED` のままなので、一覧を見ても気付けません。

## 設定の手順

1. GitHub リポジトリの **Actions** タブを開きます。
2. 左側の workflow 一覧から **Cloud Scheduler Management** を選びます。
3. 右側の **Run workflow** ボタンを押します。
4. `Action to perform` のプルダウンから、行いたい操作を選びます。
   - `create`: 新しく定期実行のタイマーを作ります（または更新します）。
   - `pause`: 定期実行を一時停止します。
   - `resume`: 一時停止したタイマーを再開します。
   - `delete`: タイマーを削除します（削除する場合は下の `confirm_delete` に `DELETE` と入力が必要です）。
   - `run`: 今すぐ1回だけ手動で動かします。
5. 今回は最初なので `create` を選んで実行します。

## 注意事項：取引戦略の変更について

Cloud Scheduler は「すでにデプロイされている Cloud Run Job のスイッチを押すだけ」の役割です。
もし取引戦略（`strategy_name`）を変更したい場合は、Scheduler を触るのではなく、もう一度 **Deploy to GCP** ワークフローを実行して Cloud Run Job 自体を更新してください。

## 完了条件チェックリスト

- [ ] Cloud Scheduler Management が緑色（成功）で終わった
- [ ] GCP のコンソールで Cloud Scheduler にジョブが登録されていることが確認できた
- [ ] [09-status-check.md](09-status-check.md) の確認を実行し、`結果: 異常 0 件` で終わった

!!! warning "「登録されている」ことは「動いている」ことではありません"

    ジョブが登録されていて状態が `ENABLED` でも、起動要求が毎回失敗していることがあります。2026-08-30 には、この2つが成立したまま、5分ごとの起動がすべて失敗し続けていました。

    **起動が成功しているかどうかは、登録の確認では分かりません。** 3つ目のチェック項目まで確認してください。

これで、GCPへのデプロイと自動実行の設定はすべて完了です。
不要になったリソースを消したい場合は [08-cleanup.md](08-cleanup.md) を参照してください。
