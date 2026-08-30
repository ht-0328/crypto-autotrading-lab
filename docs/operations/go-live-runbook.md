# 実注文を始めるまでの手順（オーナー作業）

## 文書の目的

- 実資金での自動売買を始めるまでに、**オーナー本人にしかできない作業**を順番に示す
- 各手順で「なぜ必要か」「終わったと判断する基準」を明確にする
- 途中で止まってよい区切りを示す

## 対象読者

このリポジトリのオーナー（GMOコインの口座と GCP プロジェクトを持つ人）

## 関連ドキュメント

- [実注文までの作業計画 (plans/README.md)](../plans/README.md)
- [安全ルールの数値 (overview/roadmap.md)](../overview/roadmap.md)
- [リアル取引の停止からの復旧手順 (real-trading-recovery.md)](real-trading-recovery.md)
- [GCP セットアップ手順 (gcp/README.md)](gcp/README.md)

## 前提

この手順に入る前に、次が終わっている必要があります。すべて完了済みです。

- 売り注文の自動化（買い→保有→売り→現金 のサイクルが閉じている）
- 注文数量の丸め、注文価格の基準、手数料と上限、市場データの検証、API のリトライ、重複実行の抑止
- 1日の損失上限・連敗・スリッページによる自動停止
- 注文と停止の通知（送信の仕組み。**受信の確認は未実施**）

## 作業の分担

| できる人 | 作業 |
| --- | --- |
| **オーナーのみ** | GMOコインの口座と APIキー、Discord の Webhook 作成、GCP コンソール操作、入金、初回実注文の監視 |
| AI に依頼できる | デプロイワークフローの変更、発注意図の先行保存、日次サマリーの通知、コードとドキュメントの修正 |

**AI に依頼できる残作業が2件あります。** ステップ5に入る前に依頼してください。

- **発注意図の先行保存**: 注文を送った直後・状態を保存する前にプロセスが落ちると、取引所に注文があるのにアプリ側に記録が残りません
- **日次サマリーの通知**: 1日1回、その日の損益・注文回数・保有状況を通知します

---

## ステップ1: GMOコインの口座と APIキーを用意する

### なぜ必要か

実注文には Private API のキーが要ります。権限を絞らないと、キーが漏れたときの被害が取引額と桁違いになります。

### やること

1. GMOコインにログインし、**このボット専用のサブ口座**を作れるか確認する。
   - 作れる場合はサブ口座を使う。**専用口座を使わない場合は、その口座に他の BTC を置かないでください。** アプリは自分が記録した数量までしか売りませんが、口座を分けたほうが突き合わせが単純になります。
2. API 設定画面で新しい APIキーを作る。
3. **権限は「注文」と「参照」だけにする。出金の権限は絶対に付けない。**
4. APIキーとシークレットを、パスワードマネージャなど安全な場所に控える。
   - **リポジトリにコミットしない。** `.env` にも書かない。

### 終わったと判断する基準

- APIキーとシークレットが手元にある
- そのキーに出金権限が付いていないことを、GMOコインの画面で確認した

---

## ステップ2: 通知先を用意して、実際に届くことを確認する

### なぜ必要か

手動の承認を置かない運転では、発注の直前に内容を見る機会がありません。**起きたことを事後に必ず知ることだけが、あなた側の歯止めです。** 届かない通知は無いのと同じなので、実際に受信できることを先に確かめます。

### やること

1. Discord で通知を受けるチャンネルを作る。
2. チャンネル設定 → 連携サービス → ウェブフック から Webhook を作り、URL をコピーする。
3. ローカルで動かして、通知が届くことを確認する。

   ```bash
   cd projects/crypto-autotrading-app
   export NOTIFICATION_WEBHOOK_URL="（コピーしたURL）"
   export APP_DATA_DIR=../../data
   ./gradlew run
   ```

   `config/application-gmo.yaml` の `notification.enabled` を `true` にしてから実行してください。

4. Slack を使う場合は `notification.payload_key` を `text` に変えます（Discord は `content`）。

### 終わったと判断する基準

- Discord のチャンネルに通知が届いた
- 通知の本文に Webhook の URL が含まれていない

### 注意

- **Webhook の URL は秘密情報です。** 設定ファイルに書かず、環境変数 `NOTIFICATION_WEBHOOK_URL` で渡します。
- この段階ではまだ実注文をしません。届くかどうかだけを確かめます。

---

## ステップ3: GCP 環境を作る

サービスアカウント・GCSバケット・Artifact Registry を作ります。次のステップで権限を与える相手が必要になります。

### なぜ必要か

シークレットを読む権限は、Cloud Run Job の実行サービスアカウントに与えます。**そのアカウントが存在しないと権限を与えられません。** GCP プロジェクトを作っただけでは、これらのリソースはまだありません。

### やること

1. 現在の状態を確認する。

   ```bash
   gcloud config set project "${GCP_PROJECT_ID}"
   gcloud iam service-accounts list
   ```

   `RUNTIME_SERVICE_ACCOUNT_NAME` に設定した名前のアカウントが無ければ、次に進みます。

2. GitHub Actions Variables が設定されていることを確認する。

   ```bash
   gh variable list
   ```

   最低限、`GCP_PROJECT_ID` / `GCP_REGION` / `GCP_WORKLOAD_IDENTITY_PROVIDER` / `GCP_DEPLOY_SERVICE_ACCOUNT` / `RUNTIME_SERVICE_ACCOUNT_NAME` / `BUILD_SERVICE_ACCOUNT_NAME` / `SCHEDULER_SERVICE_ACCOUNT_NAME` / `GCS_BUCKET_NAME` / `ARTIFACT_REPOSITORY` が必要です。詳細は [GitHub Actions Variables の設定](gcp/05-github-actions-variables.md) を参照してください。

   > 取引パラメータ系の Variables（`TRADING_*` など）は未設定でも構いません。未設定のときは、イメージに含まれる `config/application-gmo.yaml` の値が使われます。

3. 構築のワークフローを実行する。

   ```bash
   gh workflow run bootstrap-create-gcp.yml
   gh run watch
   ```

### 終わったと判断する基準

```bash
gcloud iam service-accounts list
gcloud storage buckets list
gcloud artifacts repositories list
```

- 実行用・ビルド用・スケジューラ用のサービスアカウントが存在する
- GCSバケットと Artifact Registry が存在する

### 注意

- 作ったリソースは [リソースのクリーンアップ手順](gcp/08-cleanup.md) で削除できます。
- バケットとレジストリは、置いておくだけならほぼ課金されません。

---

## ステップ4: GCP に認証情報を登録する

### なぜ必要か

Cloud Run で動くアプリに、APIキーと Webhook URL を安全に渡す必要があります。**現在このリポジトリには Secret Manager の設定がありません。** ここで新しく作ります。

### やること

1. Secret Manager API を有効にする。

   ```bash
   gcloud services enable secretmanager.googleapis.com --project="${GCP_PROJECT_ID}"
   ```

2. シークレットを3つ作る。

   ```bash
   printf '%s' "（GMOのAPIキー）"      | gcloud secrets create gmo-api-key       --data-file=- --project="${GCP_PROJECT_ID}"
   printf '%s' "（GMOのAPIシークレット）" | gcloud secrets create gmo-api-secret    --data-file=- --project="${GCP_PROJECT_ID}"
   printf '%s' "（Discord の Webhook URL）" | gcloud secrets create notification-webhook-url --data-file=- --project="${GCP_PROJECT_ID}"
   ```

   > `printf` を使うのは、`echo` だと末尾に改行が入り、署名の計算がずれるためです。

3. Cloud Run Job の実行サービスアカウントに、シークレットを読む権限を与える。

   ```bash
   for SECRET in gmo-api-key gmo-api-secret notification-webhook-url; do
     gcloud secrets add-iam-policy-binding "${SECRET}" \
       --member="serviceAccount:${RUNTIME_SERVICE_ACCOUNT_NAME}@${GCP_PROJECT_ID}.iam.gserviceaccount.com" \
       --role="roles/secretmanager.secretAccessor" \
       --project="${GCP_PROJECT_ID}"
   done
   ```

### 終わったと判断する基準

- `gcloud secrets list --project="${GCP_PROJECT_ID}"` に3つ表示される
- シークレットの値をターミナルの履歴やログに残していない

### 注意

- **シェルの履歴に APIキーが残ります。** 気になる場合は `history -c` するか、ファイルから `--data-file` で読ませてください。

---

## ステップ5: デプロイを Phase3 に対応させる（AI に依頼可）

### なぜ必要か

現在のデプロイワークフローは、Phase1 の安全策として**実注文を無効に固定**しています。

```yaml
# .github/workflows/deploy-gcp.yml
ENV_VARS="${ENV_VARS},REAL_TRADING_DRY_RUN=true"
ENV_VARS="${ENV_VARS},REAL_TRADING_ENABLED=false"
```

また、`APP_PHASE`、GMOの認証情報、通知先の URL をアプリに渡していません。**この状態では実注文はできません。これは意図した作りです。**

### やること

1. AI に、デプロイワークフローの変更を依頼する。必要なのは次です。
   - `APP_PHASE` を GitHub Actions Variables から渡す
   - `--set-secrets` で `GMO_API_KEY` / `GMO_API_SECRET` / `NOTIFICATION_WEBHOOK_URL` をシークレットから渡す
   - `REAL_TRADING_DRY_RUN` / `REAL_TRADING_ENABLED` を Variables から渡す（既定は安全側）
2. 変更内容をレビューし、マージする。

### 終わったと判断する基準

- デプロイが成功する
- **この時点ではまだ `APP_PHASE=1` のまま**。実注文は無効

---

## ステップ6: 本番で dry-run 検証をする

### なぜ必要か

1円も動かさずに、本番環境の配線と異常時の挙動を確かめます。ここで見つかる問題は、実資金を入れてから見つかるより桁違いに安いです。

### やること

1. GitHub Actions Variables を設定する。
   - `REAL_TRADING_DRY_RUN` = `true`
   - `REAL_TRADING_ENABLED` = `false`
   - `APP_PHASE` = `1`
2. デプロイして、Cloud Scheduler で定期実行する。
3. **最低でも数日、できれば1週間**動かす。
4. 毎日、次を確認する。

   | 確認すること | 見る場所 |
   | --- | --- |
   | 毎回正常終了しているか | Cloud Run Job の実行履歴 |
   | 秘密情報がログに出ていないか | Cloud Logging（`GMO_API_KEY` などで検索） |
   | 状態ファイルが壊れていないか | GCS の `state.json` |
   | 市場データの検証で見送りが続いていないか | Cloud Logging（`市場データの検証NG` で検索） |
   | ログやメモリが膨らんでいないか | Cloud Run のメトリクス |

### 終わったと判断する基準

- 数日間、異常終了ゼロ
- `state.json` の破損ゼロ
- 秘密情報がログに出ていない

---

## ステップ7: 緊急停止を実際に試す

### なぜ必要か

**止め方を知らないまま実資金を入れてはいけません。** 手順が書いてあることと、実際に止まることは別です。

### やること

1. Cloud Scheduler を停止する。

   ```bash
   gcloud scheduler jobs pause "${CLOUD_SCHEDULER_JOB_NAME}" --location="${GCP_REGION}"
   ```

2. 次の実行時刻を過ぎても Cloud Run Job が起動しないことを確認する。
3. 再開する。

   ```bash
   gcloud scheduler jobs resume "${CLOUD_SCHEDULER_JOB_NAME}" --location="${GCP_REGION}"
   ```

### 終わったと判断する基準

- 停止したあと、実行が起きないことを目で確認した
- 再開したあと、実行が戻ることを確認した

### 注意

- **Scheduler を止めても、すでに保有しているポジションは残ります。** 決済は GMOコインの画面から手動で行います。急いで損失を止めたい場合は、停止と並行して手動決済してください。

---

## ステップ8: 口座に入金する

### なぜ必要か

**口座に入っていないお金は失えません。これが最後の防御線です。** 上限値の実装に不具合があっても、これだけは確実に効きます。

### やること

1. GMOコインの口座に **30,000円** を入金する（[安全ルールの数値](../overview/roadmap.md)で決定済み）。
2. その口座に他の BTC が入っていないことを確認する。

### 終わったと判断する基準

- 口座残高が 30,000円
- BTC の残高が 0

---

## ステップ9: 最小額で初回の実注文を通す

### なぜ必要か

**目的は儲けることではありません。** 「買い → 保有 → 売り → 現金」が現実の取引所で記録どおりに完結することを、失っても構わない金額で確かめます。

### 着手条件（1つでも欠けたら進まない）

- [ ] ステップ1〜8 がすべて終わっている
- [ ] AI に依頼した2件（発注意図の先行保存、日次サマリーの通知）がマージ済み
- [ ] 通知が実際に届くことを確認済み
- [ ] 緊急停止を実際に試して止まることを確認済み
- [ ] APIキーに出金権限が付いていない

### やること

1. **人が画面を見ている時間帯**に作業する。夜間や外出中に始めない。
2. GitHub Actions Variables を切り替える。
   - `APP_PHASE` = `3`
   - `REAL_TRADING_DRY_RUN` = `false`
   - `REAL_TRADING_ENABLED` = `true`
3. **Cloud Scheduler は停止したまま**にする。定期実行から始めない。
4. デプロイして、Cloud Run Job を**手動で1回**実行する。

   ```bash
   gcloud run jobs execute "${CLOUD_RUN_JOB_NAME}" --region="${GCP_REGION}"
   ```

5. 買い注文が出たら、GMOコインの画面で約定を確認する。
6. **記録の3点が一致することを確認する。**

   | 見るもの | 場所 |
   | --- | --- |
   | 取引所の記録 | GMOコインの取引履歴 |
   | アプリの記録 | GCS の `state.json` |
   | 通知の内容 | Discord |

7. 売り判定が出るまで保有し、売り注文が自動で出ることを確認する。
8. 1サイクル完結したら、もう一度3点を突き合わせる。

### 終わったと判断する基準

- 買いと売りがそれぞれ取引所側で約定した
- `state.json` の保有数量・買値・確定損益が取引所の記録と一致した
- 通知の内容が実際の注文と一致した
- 上限値を超える注文が1回も出ていない

### すぐ中止する条件

次のどれかが起きたら、**Scheduler を止めて `REAL_TRADING_ENABLED` を `false` に戻してください。**

- `state.json` と取引所の記録が食い違った
- 同じ判定で2回注文が出た
- 売り注文が出るべき場面で出なかった
- 上限値を超える注文が出た
- 通知が来ない、または内容が実際と違う

---

## ステップ10: 定期実行に戻す

### なぜ必要か

ここまでは人が見ている時間の手動実行です。定期実行に戻して初めて「自動売買」になります。

### 着手条件

- ステップ8 のサイクルが、不整合ゼロで規定回数（[ロードマップ](../overview/roadmap.md) の Phase4 着手条件）連続で成立している

### やること

1. Cloud Scheduler を再開する。
2. 最初の数日は毎日、通知と `state.json` を確認する。
3. **外部からの監視を用意する。** アプリは「自分が動いていないこと」を通知できません。ジョブが起動しなくなったとき、通知は「来ない」だけです。日次サマリーが届かないことに気付ける仕組みが要ります。

### 終わったと判断する基準

- 定期実行で一定期間回り続け、記録の3点が一致し続けている
- ジョブが起動しなくなった場合に気付ける

---

## 途中で止まってよい区切り

急ぐ必要はありません。次の区切りで止めても、次に再開するときに困りません。

| 区切り | 状態 |
| --- | --- |
| ステップ2まで | 通知が届くことを確認済み。お金は動かない |
| ステップ4まで | GCP環境と認証情報が揃った。お金は動かない |
| ステップ6まで | 本番で dry-run が安定稼働。お金は動かない |
| ステップ8まで | 準備完了。まだ実注文はしていない |
| ステップ9まで | 実注文を1サイクル確認済み。定期実行はしていない |
