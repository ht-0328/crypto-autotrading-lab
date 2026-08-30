# 5. GitHub Actions の Variables 設定

## 文書の目的

- GCP に接続するための情報を、GitHub 側に登録する方法

## 対象読者

運用インフラ構築担当者

## 関連ドキュメント

- [06-deploy-cloud-run-job.md](06-deploy-cloud-run-job.md)

## 概要

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

| Name                             | 設定する値の例                                                            |
| -------------------------------- | ------------------------------------------------------------------------- |
| `GCP_PROJECT_ID`                 | `あなたのGCPプロジェクトID`                                               |
| `GCP_REGION`                     | `asia-northeast1` (東京リージョンの場合)                                  |
| `GCP_WORKLOAD_IDENTITY_PROVIDER` | `手順1でメモした長い文字列`                                               |
| `GCP_DEPLOY_SERVICE_ACCOUNT`     | `github-actions-deploy@あなたのGCPプロジェクトID.iam.gserviceaccount.com` |
| `ARTIFACT_REPOSITORY`            | `crypto-app-repo` (好きな名前)                                            |
| `IMAGE_NAME`                     | `crypto-autotrading-app` (好きな名前)                                     |
| `GCS_BUCKET_NAME`                | `crypto-data-bucket-あなたのGCPプロジェクトID` (世界で一意な名前)         |
| `CLOUD_RUN_JOB_NAME`             | `crypto-trading-job` (好きな名前)                                         |
| `BUILD_SERVICE_ACCOUNT_NAME`     | `crypto-build-sa` (※1)                                                    |
| `RUNTIME_SERVICE_ACCOUNT_NAME`   | `crypto-runtime-sa` (※1)                                                  |
| `CLOUD_SCHEDULER_JOB_NAME`       | `crypto-scheduler-job` (好きな名前)                                       |
| `SCHEDULER_SERVICE_ACCOUNT_NAME` | `crypto-scheduler-sa` (※1)                                                |
| `SCHEDULER_CRON`                 | `*/5 * * * *` (5分ごとの場合)                                             |
| `SCHEDULER_TIME_ZONE`            | `Asia/Tokyo`                                                              |
| `TRADING_SYMBOL`                 | `BTC`                                                                     |

!!! note "（※1）サービスアカウント名に関する注意"

    `BUILD_SERVICE_ACCOUNT_NAME` などの値は、メールアドレスではなく「ID部分のみ（6〜30文字）」を指定してください。（例: `crypto-build-sa`）

### アプリの動作設定（任意）

以下は [deploy-gcp.yml](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/.github/workflows/deploy-gcp.yml) が Cloud Run Job の環境変数として渡すものです。**未登録でも構いません。** その場合は空文字が渡り、コンテナに同梱された [config/application-gmo.yaml](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/config/application-gmo.yaml) の値が使われます。

!!! warning "実注文と通知の設定は、この扱いではありません"

    次の節で扱う `APP_PHASE`、`REAL_TRADING_ENABLED`、`REAL_TRADING_DRY_RUN`、`NOTIFICATION_ENABLED` の4つだけは、未登録のとき設定ファイルの値ではなく**安全側の値に固定されます**。詳しくは次の節を読んでください。

| Name                             | 設定する値の例                                                            |
| -------------------------------- | ------------------------------------------------------------------------- |
| `API_PUBLIC_BASE_URL`            | `https://api.coin.z.com/public`                                           |
| `API_PRIVATE_BASE_URL`           | `https://api.coin.z.com/private`                                          |
| `API_RETRY_COUNT`                | `3`                                                                       |
| `APP_INTERVAL`                   | `5min`                                                                    |
| `OUTPUT_PATH`                    | `trades.csv`                                                              |
| `STATE_PATH`                     | `state.json`                                                              |
| `TRADING_INITIAL_CAPITAL`        | `10000`                                                                   |
| `TRADING_TRADE_AMOUNT`           | `1000`                                                                    |
| `TRADING_BUY_THRESHOLD`          | `0.005`                                                                   |
| `TRADING_SELL_THRESHOLD`         | `0.005`                                                                   |
| `TRADING_VOLATILITY_THRESHOLD`   | `0.003`                                                                   |
| `TRADING_SHARP_CHANGE_THRESHOLD` | `0.01`                                                                    |
| `TRADING_ORDER_SIZING_MODE`      | `FIXED_AMOUNT`                                                            |
| `TRADING_COOLDOWN_LENGTH`        | `12`                                                                      |
| `TRADING_ATR_LENGTH`             | `14`                                                                      |
| `TRADING_ATR_PROFIT_MULTIPLIER`  | `2.0`                                                                     |
| `TRADING_ATR_LOSS_MULTIPLIER`    | `2.0`                                                                     |

!!! warning "API_BASE_URL は使われません"

    以前は `API_BASE_URL` を登録する運用でしたが、アプリはこの名前を読みません。`API_PUBLIC_BASE_URL` と `API_PRIVATE_BASE_URL` に登録し直し、古い `API_BASE_URL` は削除してください。

!!! note "取引戦略（strategy_name）について"

    取引戦略はここでは設定しません。デプロイを実行する際（workflow_dispatch）の画面で選択します。

!!! warning "秘密情報について"

    API キーやシークレットなどの秘密情報は、ここ（Variables）ではなく、隣の **Secrets** タブに登録してください。

### 実注文と通知の設定（安全に関わる）

ここで扱う4つは、**実資金を動かすかどうかと、異常に気付けるかどうかを決めます。** 未登録のときは設定ファイルの値ではなく、安全側の値が使われます。

| Name | 未登録のときの値 | 何が変わるか |
| --- | --- | --- |
| `APP_PHASE` | `1` | 実注文が許可されるのは `3` 以上。`3` 未満で実注文を有効にすると、アプリは起動時に失敗する |
| `REAL_TRADING_ENABLED` | `false` | `true` で実発注の経路が有効になる。GMO の APIキーが Secret Manager から渡されるのもこのときだけ |
| `REAL_TRADING_DRY_RUN` | `true` | `true` の間は、判断はするが注文を送らない |
| `NOTIFICATION_ENABLED` | `false` | `true` で注文・停止・日次サマリーが通知される。Webhook URL が Secret Manager から渡されるのもこのときだけ |

**実資金で発注されるのは、`APP_PHASE` が 3 以上、`REAL_TRADING_ENABLED` が `true`、`REAL_TRADING_DRY_RUN` が `false` の3つがそろったときだけです。** 1つでも欠ければ発注されません。

上限や停止条件は、未登録なら設定ファイルの値が使われます。

| Name | 設定する値の例 | 意味 |
| --- | --- | --- |
| `REAL_TRADING_MAX_ORDER_JPY` | `2000` | 1回あたりの注文金額の上限 |
| `REAL_TRADING_MAX_DAILY_ORDER_JPY` | `4000` | 1日の累計注文金額の上限 |
| `REAL_TRADING_MAX_POSITION_JPY` | `2000` | 保有金額と注文予定額の合計の上限 |
| `REAL_TRADING_MAX_DAILY_LOSS_JPY` | `1000` | この額まで負けたら、その日は新規の買いを止める |
| `REAL_TRADING_MAX_CONSECUTIVE_LOSSES` | `3` | この回数だけ連敗したら、その日は新規の買いを止める |
| `REAL_TRADING_MAX_SLIPPAGE_RATE` | `0.005` | 想定価格からの乖離の許容幅 |
| `REAL_TRADING_MIN_ORDER_SIZE` | `0.00001` | 取引所が定める最小注文数量 |
| `REAL_TRADING_SIZE_STEP` | `0.00001` | 取引所が定める注文数量の刻み |
| `REAL_TRADING_TAKER_FEE_RATE` | `0.0005` | 成行注文の手数料率。上限の判定に含める |
| `NOTIFICATION_PAYLOAD_KEY` | `content` | 通知の本文を入れるキー。Discord は `content`、Slack は `text` |

!!! danger "有効にしたのに認証情報が無いと、デプロイが失敗します"

    `REAL_TRADING_ENABLED` を `true` にすると、デプロイは Secret Manager の `gmo-api-key` と `gmo-api-secret` を Cloud Run Job に結び付けます。`NOTIFICATION_ENABLED` を `true` にすると `notification-webhook-url` を結び付けます。

    **登録されていないシークレットを指定するとデプロイは失敗します。** これは意図した動作です。実注文や通知を有効にしたのに認証情報が無い状態は、動き出す前に気付くべき設定漏れだからです。

## 完了条件チェックリスト

- [ ] 必要な設定値がすべて GitHub の Variables に登録された
- [ ] 実資金で動かす意図が無い場合、`REAL_TRADING_ENABLED` と `REAL_TRADING_DRY_RUN` が意図した値になっている（未登録なら安全側）

終わったら、次は [06-deploy-cloud-run-job.md](06-deploy-cloud-run-job.md) に進んでください。
