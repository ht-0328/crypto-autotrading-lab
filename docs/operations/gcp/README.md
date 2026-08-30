# GCP 運用・デプロイガイド

このディレクトリには、GitHub Actions から GCP へシステムをデプロイし、運用するための手順書が順番に格納されています。
初めて構築する場合は、番号順に読み進めてください。

## 手順書一覧

- [01-account-and-project.md](01-account-and-project.md): GCPアカウントとプロジェクトの準備
- [02-gcloud-cli.md](02-gcloud-cli.md): gcloud CLI の準備とログイン
- [03-workload-identity-federation.md](03-workload-identity-federation.md): GitHub Actions との安全な連携設定
- [04-service-accounts-and-iam.md](04-service-accounts-and-iam.md): デプロイ用アカウントの作成と権限設定
- [05-github-actions-variables.md](05-github-actions-variables.md): GitHub への設定値登録
- [06-deploy-cloud-run-job.md](06-deploy-cloud-run-job.md): Cloud Run Job へのデプロイ実行
- [07-scheduler.md](07-scheduler.md): Cloud Scheduler による定期実行設定
- [09-status-check.md](09-status-check.md): デプロイ状態の確認（07 のあと、および運用中いつでも）
- [08-cleanup.md](08-cleanup.md): （不要になった場合の）リソース削除手順

!!! note "番号と読む順番"

    09 は 08 より後の番号ですが、読む順番では 07 の次です。08 は撤収するときだけ読みます。

## ワークフローの地図

GCP を操作するワークフローは5つあります。**それぞれが何を作り、飛ばすとどうなるかを把握してから実行してください。**

| ワークフロー | 何をするか | いつ実行するか | 飛ばすとどうなるか |
| --- | --- | --- | --- |
| **Bootstrap Create GCP Resources** | Artifact Registry、GCS バケット、ビルド用・実行用のサービスアカウントを作る | 最初の1回。環境を作り直したとき | デプロイが最初の確認ステップで止まる |
| **Bootstrap Grant IAM Permissions** | 上で作ったサービスアカウントに権限を付ける | Create の直後 | ビルドやデプロイの途中で `Permission Denied` になる |
| **Deploy to GCP** | イメージをビルドし、Cloud Run Job を作成・更新する | コード、設定、取引戦略を変えたとき | 古いイメージのまま動き続ける |
| **Cloud Scheduler Management** | スケジューラ用サービスアカウントの作成、`roles/run.invoker` の付与、定期実行タイマーの作成・更新 | デプロイのあと。環境を作り直したとき | **定期実行が動かない。しかも失敗に気付けない** |
| **Deployment Status Check** | いま実際に動いているかを確認する | 上のどれかを実行したあと。運用中いつでも | 動いていないことに気付けない |

### 初めて構築するときの順番

1. **Bootstrap Create GCP Resources**（[04-service-accounts-and-iam.md](04-service-accounts-and-iam.md)）
2. **Bootstrap Grant IAM Permissions**（同上）
3. **Deploy to GCP**（[06-deploy-cloud-run-job.md](06-deploy-cloud-run-job.md)）
4. **Cloud Scheduler Management** の `create`（[07-scheduler.md](07-scheduler.md)）
5. **Deployment Status Check**（[09-status-check.md](09-status-check.md)）

環境を作り直すときも同じ順番です。詳しくは [08-cleanup.md](08-cleanup.md) の「クリーンアップ後の注意（再構築について）」を参照してください。

## 気をつけること

### デプロイが成功しても、動いているとは限らない

**Deploy to GCP が緑色で終わることは、「Cloud Run Job の設定が更新された」という意味しかありません。** 定期実行が動いているかどうかは、そこには表れません。

2026-08-30 に、次の状態が発生しました。

- デプロイは成功していた
- Cloud Scheduler にジョブは登録され、状態は `ENABLED` だった
- それでも5分ごとの起動要求はすべて失敗し、売買は一度も動いていなかった

原因は、スケジューラ用サービスアカウントが存在しなかったことです。**このサービスアカウントを作るのは Cloud Scheduler Management だけで、Bootstrap Create は作りません。**

実行のたびに [09-status-check.md](09-status-check.md) の確認まで行ってください。

### 実資金が動く条件は3つそろったときだけ

`APP_PHASE` が 3 以上、`REAL_TRADING_ENABLED` が `true`、`REAL_TRADING_DRY_RUN` が `false`。この3つがそろったときだけ実発注されます。GitHub Variables に登録していない場合は、安全側の値（Phase1・無効・dry-run）が使われます。詳しくは [05-github-actions-variables.md](05-github-actions-variables.md) を参照してください。

### 取引戦略を変えるときはデプロイをやり直す

Cloud Scheduler は「すでにデプロイされている Cloud Run Job のスイッチを押すだけ」です。取引戦略（`strategy_name`）を変えるときは、Scheduler ではなく **Deploy to GCP** を実行してください。

### 止めたいときは Scheduler を止める

急いで自動売買を止めたいときは、**Cloud Scheduler Management** の `pause` を使います。ただし**すでに保有しているポジションは残ります。** 手順は [実注文を始めるまでの手順 (../go-live-runbook.md)](../go-live-runbook.md) のステップ7にあります。
