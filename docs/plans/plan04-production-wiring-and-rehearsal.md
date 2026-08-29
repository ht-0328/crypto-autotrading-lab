# PLAN04: 本番へ配線し、実注文なしでリハーサルする

**状態**: 未着手 / 前提: [PLAN01](plan01-real-sell-order.md), [PLAN02](plan02-order-safety-guards.md), [PLAN03](plan03-notification.md)

## なぜやるか

コードが正しくても、本番環境の配線と異常系の挙動を確かめずに実資金を入れることはできません。この計画は **1円も動かさずに、実注文の一歩手前まで全部通す**ためのものです。

## ゴール

本番の GCP 環境で、本物の GMO Private API に対して認証・残高照会が通り、`dry_run: true` のまま数日間安定して回っている。障害を起こしても安全側に倒れることを確認済み。

## 含む作業

### A. 認証情報の配線

- GMO API キー / シークレットを GCP Secret Manager に登録し、Cloud Run Job に渡す。
- 取引所側の **API キーの権限を最小限にする**（現物の注文と参照のみ。出金権限は付けない）。出金権限のあるキーが漏れた場合の被害は取引額と桁が違います。
- ログに出ないことを、実際のログで確認する（[PR03](../improvements/pr03-private-api-log-leak.md) の対応が効いていること）。

### B. Phase ガードの扱い

[Main.kt](../../projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/presentation/Main.kt) のガードは `app.phase < 3` かつ実注文有効のときに落ちます。**この計画では `dry_run: true` のままなので、ガードには当たりません。** `app.phase` を 3 に変えるかどうかは [PLAN05](plan05-canary-with-real-money.md) の判断です。ここでは変えないでください。

### C. 状態の永続化と排他

- [deploy-gcp.yml](../../.github/workflows/deploy-gcp.yml) は GCS を `/mnt/gcs` にマウントしています。GCS の FUSE マウントは**原子的な置き換えに対応していません**（[StateRepository](../../projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/infrastructure/output/StateRepository.kt) がフォールバックしています）。書き込み中にジョブが落ちたとき、`state.json` が壊れないことを実際に確認してください。
- [PLAN02](plan02-order-safety-guards.md) で入れた重複実行の抑止が、本番の Cloud Run Job で実際に効くことを確認する。

### D. 障害を起こしてみる（リハーサル）

WireMock またはステージング設定で、次を意図的に起こして挙動を確認します。

| 起こすこと | 期待する挙動 |
| --- | --- |
| Private API のタイムアウト | 発注 POST を再送しない。次回に注文照会で照合する |
| API がエラーを返す | 新規注文を止め、通知が飛ぶ |
| ジョブの二重起動 | 片方だけが処理し、二重に発注・保存しない |
| 部分約定 | 約定した数量だけが state に反映される |
| 状態保存の失敗 | 検知して止まる。次回に不整合を持ち越さない |
| 発注直後のプロセス強制終了 | 再起動後、注文照会で照合してから進む。二重発注しない |
| 日付をまたぐ | 日次注文上限がリセットされる |
| 朝6時をまたぐ | 判定がスキップされない |

### E. 長期 dry-run

- `app.phase: 1`（または 2）、`dry_run: true` のまま、本番スケジュールで最低数日〜1週間動かす。
- 確認すること: 毎回正常終了しているか、通知と heartbeat が来ているか、`state.json` が壊れていないか、メモリやログが膨らんでいないか（[ロードマップ](../overview/roadmap.md) の Phase1 未解決リスク）。

## 受け入れ条件

- 本番環境で GMO Private API の残高照会が成功する（**注文は送らない**）。
- ログとCSVに秘密情報が出ていないことを、実際の出力で確認済み。
- 上表の障害シナリオすべてで、期待どおり安全側に倒れる。
- 数日間の連続稼働で、異常終了ゼロ、`state.json` の破損ゼロ。
- 通知と heartbeat が届き続けている。

## 検証手順

```bash
cd projects/crypto-autotrading-app
./gradlew build
```

加えて、GCP 上での実行ログ・通知・`state.json` を人が確認します。

## やらないこと

- `dry_run: false` にすること。`app.phase` を 3 にすること。**この計画では絶対に行いません。**
