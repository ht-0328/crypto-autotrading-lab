# 7. Cloud Scheduler による定期実行の設定

| 項目 | 内容 |
| --- | --- |
| 想定読者 | 定期実行を設定する運用担当者 |
| 読んだあとできること | Cloud Scheduler で Cloud Run Job を定期実行できる |
| 状態 | 現行 |
| 機密区分 | 公開可 |
| 作成者 | リポジトリ管理者 |
| 保守責任者 | リポジトリ管理者 |
| 最終確認日 | 2026-08-30 |


## 文書の目的

- デプロイした Cloud Run Job を定期的に（例：5分ごと）動かす方法

## 対象読者

運用担当者

## 関連ドキュメント

- [06-deploy-cloud-run-job.md](06-deploy-cloud-run-job.md)

## 概要

Cloud Run Job は「1回だけ動いて終わる」仕組みです。
動かし続けるには、**Cloud Scheduler** で定期的に呼び出します。
これも GitHub Actions から設定できます。

## 設定の手順

1. GitHub リポジトリの **Actions** タブを開きます。
2. **Cloud Scheduler Management** を選びます。
3. 右側の **Run workflow** ボタンを押します。
4. `Action to perform` のプルダウンから、行いたい操作を選びます。
   - `create`: 新しく定期実行のタイマーを作ります（または更新します）。
   - `pause`: 定期実行を一時停止します。
   - `resume`: 一時停止したタイマーを再開します。
   - `delete`: タイマーを削除します（削除する場合は下の `confirm_delete` に `DELETE` と入力が必要です）。
   - `run`: 今すぐ1回だけ手動で動かします。
5. 今回は最初なので `create` を選んで実行します。

## 取引戦略を変えるときの注意

Cloud Scheduler の役割は、Cloud Run Job を呼び出すことだけです。
取引戦略を変えたい場合は、Scheduler を触りません。
**Deploy to GCP** をもう一度実行してください。Job 自体が更新されます。

## 完了条件チェックリスト

- [ ] Cloud Scheduler Management が緑色（成功）で終わった
- [ ] Cloud Scheduler にジョブが登録されていることを確認した

これで、GCPへのデプロイと自動実行の設定はすべて完了です！
不要になったリソースを消したい場合は [08-cleanup.md](08-cleanup.md) を参照してください。
