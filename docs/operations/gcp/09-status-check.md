# 9. デプロイ状態の確認

## 文書の目的

- デプロイした環境が、いま実際にどのモードで動いているかを確認する方法を示す
- 「デプロイは成功しているのに動いていない」状態を見つける

## 対象読者

運用担当者

## 関連ドキュメント

- [06-deploy-cloud-run-job.md](06-deploy-cloud-run-job.md)
- [07-scheduler.md](07-scheduler.md)
- [実注文を始めるまでの手順 (../go-live-runbook.md)](../go-live-runbook.md)

## 概要

**デプロイが成功したことと、システムが動いていることは別です。**

2026-08-30 に、次の状態が発生しました。

- Cloud Run Job のデプロイは成功していた
- Cloud Scheduler にジョブは登録され、状態は `ENABLED` だった
- それでも5分ごとの起動要求はすべて失敗し、売買は一度も動いていなかった

このとき、デプロイのログにも Cloud Run Job の設定にも異常は出ません。気付くには、起動要求が成功しているかを別に確認する必要があります。

そのための確認を [check-deployment-status.sh](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/scripts/ops/check-deployment-status.sh) にまとめてあります。確認するのは次の4点です。

1. Cloud Run Job が実注文モードか、安全モード（dry-run または無効）か
2. 実注文と通知に必要な認証情報が Secret Manager から結線されているか
3. Cloud Scheduler が実際にジョブを起動できているか
4. 直近の実行が成功しているか、自動実行が止まっていないか

確認できなかった項目は、成功扱いにせず異常として扱います。

## 使い方

### GitHub Actions から実行する

1. GitHub リポジトリの **Actions** タブを開きます。
2. 左側の workflow 一覧から **Deployment Status Check** を選びます。
3. **Run workflow** ボタンを押します。
4. 実行が終わったら、実行結果の画面に出る **Summary** を読みます。

異常が1件でもあると、このワークフローは失敗（赤）で終わります。

### 手元から実行する

`gh` にログインした状態で、リポジトリのルートで実行します。

```bash
bash scripts/ops/check-deployment-status.sh
```

プロジェクトIDやジョブ名は GitHub Variables から取得するため、引数は要りません。個別に変えたい場合だけ環境変数で渡します。

```bash
CLOUD_RUN_JOB_NAME=別のジョブ名 bash scripts/ops/check-deployment-status.sh
```

## 出力の読み方

| 印 | 意味 | すること |
| --- | --- | --- |
| `[OK]` | 想定どおり | なし |
| `[情報]` | 設定値の表示 | 意図した値かどうかを目で確かめる |
| `[警告]` | 異常ではないが、意識しておく状態 | 内容を読んで、意図した状態かを判断する |
| `[異常]` | 動作に支障がある | 対処する。終了コードは 1 になる |

最後に `結果: 異常 0 件 / 警告 1 件` のように件数が出ます。

!!! warning "実注文モードの警告は消えません"

    実資金で発注する設定（`APP_PHASE` が 3 以上、`REAL_TRADING_ENABLED=true`、`REAL_TRADING_DRY_RUN=false`）で動いている間、警告が1件出続けます。これは異常ではありません。**消すべきものではなく、常に見えているべきもの**として警告にしています。

## 異常が出たときの対処

| 出力 | 意味 | 対処 |
| --- | --- | --- |
| 起動用サービスアカウントが存在しません | スケジューラがジョブを起動できない。起動要求は 404（`status.code=5`）で失敗する | [07-scheduler.md](07-scheduler.md) の手順を `create` で実行する |
| 起動用サービスアカウントに `roles/run.invoker` がありません | 起動要求は 403（`status.code=7`）で失敗する | [07-scheduler.md](07-scheduler.md) の手順を `create` で実行する |
| 直近 30 分間、スケジューラによる自動実行がありません | 定期実行が止まっている | 上の2件を先に確認する。どちらも `[OK]` なら Cloud Run Job のログを見る |
| 実注文が有効なのに `APP_PHASE` が 3 未満です | アプリは起動時に失敗する | `APP_PHASE` を 3 以上にして [06-deploy-cloud-run-job.md](06-deploy-cloud-run-job.md) を再実行する |
| 実注文が有効なのに GMO の認証情報が結線されていません | 発注できない | Secret Manager への登録を [実注文を始めるまでの手順 (../go-live-runbook.md)](../go-live-runbook.md) で確認する |
| 通知が有効なのに Webhook URL が結線されていません | 異常が起きても通知が届かない | 同上 |
| Cloud Run Job が見つからないか、参照する権限がありません | デプロイされていない、または権限不足 | [06-deploy-cloud-run-job.md](06-deploy-cloud-run-job.md) を実行する |

## 完了条件チェックリスト

- [ ] `結果: 異常 0 件` で終わった
- [ ] 表示されたモード（実注文モードか安全モードか）が、意図した状態と一致している
- [ ] 実注文モードで運用する場合、上限の表示（1回・1日・ポジション・日次損失）が意図した値と一致している
