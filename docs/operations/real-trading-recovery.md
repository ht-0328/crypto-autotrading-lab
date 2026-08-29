# リアル取引の停止からの復旧手順

## 文書の目的

- `state.json` の `realTrading.isStopped` が `true` になったときの確認手順
- 新規注文を再開するための手順

## 対象読者

運用担当者

## 関連ドキュメント

- [リアル購入処理（GMOコイン） 仕様書](../specifications/features/real-trading-gmo-order.md)
- [リアル購入処理 詳細設計書](../architecture/real-trading-gmo-order-detailed-design.md)
- [Cloud Run Job へのデプロイ](gcp/06-deploy-cloud-run-job.md)

## 概要

リアル注文の処理中に例外が発生すると、アプリは `state.json` の `realTrading.isStopped` を `true` にして以降の新規注文を止めます。**復旧は自動では行われません。** 人が状況を確認し、手動でフラグを戻す必要があります。

!!! warning "Phase1 では実注文を行いません"

    この手順は Phase3 以降で実注文を有効にした場合、または `real_trade_enabled: true` の環境で問題が起きた場合に使います。Phase1 では起動時ガードにより実注文経路に入らないため、この状態にはなりません。

## いつ停止するか

`RealTradingService` が実注文処理中に例外を捕捉したときに停止します。具体的には次のような場合です。

- GMO Private API の呼び出しが失敗した（通信エラー、認証エラー、APIエラーレスポンス）
- レスポンスの解析に失敗した
- 注文送信後の約定確認処理で例外が発生した

停止時には次が記録されます。

| フィールド | 内容 |
| --- | --- |
| `realTrading.isStopped` | `true` |
| `realTrading.stopReason` | 停止のきっかけになった例外のメッセージ |
| `realTrading.stoppedAt` | 停止した日時（Asia/Tokyo） |
| `realTrading.latestOrder` | 停止時点で把握していた最新の注文情報 |

## ロックファイルが残っている場合

実行が異常終了すると、状態ファイルの隣に `state.json.lock` が残ることがあります。この状態では以降の実行がすべてスキップされます。

- **15分を過ぎたロックは、次の実行が自動で引き継ぎます。** 通常は手を入れる必要はありません。
- 15分待てない場合は、**動いている実行がないことを確認してから**ロックファイルを削除してください。動いている実行のロックを消すと、2つの実行が同時に注文を出す可能性があります。

## 手順

### Step 1: 停止していることを確認する

Cloud Run の実行ログで「安全チェックNG: realTrading.isStopped=true」が出ていれば停止中です。

```bash
gcloud run jobs executions list --job ${CLOUD_RUN_JOB_NAME} --region ${GCP_REGION}
gcloud logging read "resource.type=cloud_run_job" --limit 50
```

### Step 2: state.json を取得する

```bash
gcloud storage cp gs://${GCS_BUCKET_NAME}/data/state.json ./state.json
cat ./state.json
```

保存先は `APP_DATA_DIR` と `output.state_path` の組み合わせで決まります。既定では `/mnt/gcs/data/state.json`（GCS バケット上の `data/state.json`）です。

### Step 3: 停止理由と注文の状況を確認する

`state.json` の次を確認します。

- `realTrading.stopReason` と `realTrading.stoppedAt`: 何が起きたか、いつか
- `realTrading.latestOrder`: 最後に扱った注文の `orderId` と `status`

**再開する前に、取引所側の実態と突き合わせてください。**

- GMOコインの取引画面またはアプリで、未約定の注文が残っていないか
- 実際の保有数量が `state.json` の `holdingAmount` と一致しているか
- 実際の JPY 残高が想定どおりか

不一致がある場合は、フラグを戻す前に `state.json` の `isHolding` / `holdingAmount` / `buyPrice` を実態に合わせて修正します。**ここを飛ばすと、二重注文や誤った損切り判定につながります。**

### Step 4: 停止フラグを戻す

```json
{
  "realTrading": {
    "isStopped": false,
    "stopReason": null,
    "stoppedAt": null
  }
}
```

`latestOrder` は、取引所側で完了が確認できていれば `status` を `EXECUTED` または `CANCELED` に更新するか、`null` にします。未確認のまま残すと、次回の実行で「未確認または受付中の注文が存在」として再び注文が見送られます。

### Step 5: 書き戻して再実行する

```bash
gcloud storage cp ./state.json gs://${GCS_BUCKET_NAME}/data/state.json
gcloud run jobs execute ${CLOUD_RUN_JOB_NAME} --region ${GCP_REGION} --wait
```

### Step 6: 再開できたことを確認する

実行ログに「安全チェックNG: realTrading.isStopped=true」が出ていないことを確認します。

## 完了条件チェックリスト

- [ ] 停止理由を確認し、原因に対処した
- [ ] 取引所側の保有数量・未約定注文と `state.json` の内容が一致している
- [ ] `isStopped` を `false` に戻して書き戻した
- [ ] 再実行して、停止によるスキップが出ないことを確認した

## 注意

- **原因が分からないまま `isStopped` を戻さないでください。** 同じ失敗を繰り返し、注文が重複する可能性があります。
- 書き戻す前に、取得した `state.json` のバックアップを取ってください。
