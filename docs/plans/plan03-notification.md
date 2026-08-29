# PLAN03: 通知を実装する（Phase2a）

**状態**: 未着手 / 前提: [PLAN00](plan00-phase-and-safety-contract.md)

## なぜやるか

実資金を動かすとき、**人間が異常に気づけないことが最大のリスク**です。現在このリポジトリには通知の実装が1つもありません（`grep` で確認済み）。分かるのは Cloud Logging を自分で見に行ったときだけです。

- 注文が失敗して `isStopped=true` になっても、誰も気づかない。復旧は手動でしかできないのに、止まったことが伝わりません（[復旧手順](../operations/real-trading-recovery.md)）。
- 未確認注文を抱えたまま止まっても、気づかない。
- **手動承認を置かない方針を取ったため（[PLAN00](plan00-phase-and-safety-contract.md)）、通知の重要度はさらに上がります。** 発注の直前に人間が内容を見るタイミングが無いので、「起きたことを事後に必ず知る」ことだけが人間側の歯止めになります。

Web画面は作りません。[PLAN00](plan00-phase-and-safety-contract.md) の決定どおり、Phase2 のうち通知だけを先に出します。

## ゴール

売買と異常が起きたことを人間が即座に知り、必要なら止められる。

## 含む作業

### A. 通知の送信

- 送信先を決める（LINE Messaging API、Discord Webhook など）。**Webhook URL やトークンは秘密情報**です。[AGENTS.md](../../AGENTS.md) のとおり、コードにも設定ファイルにも書かず、環境変数と Secret Manager 経由で渡してください。
- 通知する内容に、APIキー・シークレット・署名・個人情報を含めない。
- 通知の送信に失敗しても、**売買処理そのものは落とさない**（通知はあくまで観測手段）。ただし失敗はログに残す。

### B. 通知するイベント

| イベント | 通知する内容 |
| --- | --- |
| 実注文の送信 | side、数量、想定金額、判定理由 |
| 約定の確認 | 約定価格、数量、確定損益 |
| 安全チェックによる見送り | 満たさなかった条件 |
| `isStopped=true` への遷移 | 停止理由、未確認注文の有無、復旧手順へのリンク |
| 未確認注文の持ち越し | orderId、経過時間 |
| 日次サマリー | 損益、注文回数、現在の保有 |
| heartbeat（定期実行が生きていること） | 最終実行時刻 |

**heartbeat は忘れがちですが重要です。** ジョブが起動しなくなった場合、通知は「来ない」だけなので、異常が沈黙として現れます。定期的な生存通知が無いと、止まったことに気づけません。

### C. 自動停止の条件（手動承認の代替）

[PLAN00](plan00-phase-and-safety-contract.md) で手動承認を置かないと決めたので、**人間の代わりにシステムが自分で止まる**必要があります。次を実装し、発動したら必ず通知してください。

| 条件 | 閾値 | 挙動 |
| --- | --- | --- |
| 1日の損失が上限に達した | 2,000円 | その日は新規の買いを止める |
| 連敗が規定回数に達した | 3連敗 | 新規の買いを止める |
| 約定価格が想定から規定率以上乖離した | 0.5% | 新規の買いを止める |
| API がエラーを返した / 未確認注文が残った | — | 新規の買いを止める（既存の `isStopped`） |

閾値の正は [ロードマップ](../overview/roadmap.md) の「安全ルール（数値）」です。ここに書き写した値がズレたら、ロードマップ側を正としてください。

**いずれも「新規の買いを止める」であって、「保有解消の売りを止める」ではありません。** 売りまで止めると損切り不能になります（[PLAN01](plan01-real-sell-order.md) の論点3）。

### D. 緊急停止（kill switch）

**追加の実装は不要です。** [PLAN00](plan00-phase-and-safety-contract.md) で「Cloud Scheduler の停止と設定フラグ」を正と決めました。手順は [ロードマップ](../overview/roadmap.md) の「緊急停止（kill switch）」にあります。**実際に一度試して、止まることを確認してください**（[PLAN05](plan05-canary-with-real-money.md) の着手条件）。

## 変更対象

| ファイル | 変更内容 |
| --- | --- |
| `domain/notification/`（新規） | 通知イベントと送信のインターフェース |
| `infrastructure/notification/`（新規） | 送信先ごとの実装 |
| [TradingApplication.kt](../../projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/application/TradingApplication.kt) / [RealTradingService.kt](../../projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/realtrading/RealTradingService.kt) | 通知イベントの発火 |
| [config/](../../config/) | 通知の有効/無効と送信先の種類、自動停止の閾値（**トークンは書かない**） |
| [roadmap.md](../overview/roadmap.md) | Phase2a の完了を記録 |

**レイヤの置き場所に注意してください。** 外部への送信は infrastructure です。domain から直接 HTTP を叩かないこと。着手前に [kotlin-layer-boundaries](../../.agents/skills/kotlin-layer-boundaries/SKILL.md) を読んでください。

## 受け入れ条件

- 上表のイベントで通知が飛ぶ（単体テストで送信呼び出しを検証）。
- 通知本文に秘密情報が含まれない（テストで検証）。
- 通知の送信失敗でアプリが異常終了しない。
- heartbeat が定期実行のたびに送られる。
- 自動停止の各条件が発動して新規の買いが止まり、通知が飛ぶ（単体テストで検証）。
- 自動停止が発動しても、保有解消の売りは止まらない。
- 実際に自分の端末へテスト通知が届くことを手で確認する。

## 検証手順

```bash
cd projects/crypto-autotrading-app
./gradlew build
```

加えて、実際の送信先へテスト通知を1回送って受信を確認します。

## やらないこと

- Web画面、ダッシュボード、画面からの設定変更（Phase2b）。
- 手動承認の仕組み（[PLAN00](plan00-phase-and-safety-contract.md) で実装しないと決定）。
