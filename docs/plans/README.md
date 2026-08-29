# 実注文までの作業計画

## 文書の目的

- 「実資金で自動売買が回っている」状態に到達するまでの作業を、着手できる単位に分けて管理する
- どの順序で進めるか、なぜその順序なのかを示す
- 実資金を投入してよい条件を明示する

## 対象読者

プロジェクト管理者、開発メンバー、AIコーディングエージェント

## 関連ドキュメント

- [ロードマップと完了条件 (overview/roadmap.md)](../overview/roadmap.md)
- [改善計画 (improvements/README.md)](../improvements/README.md)
- [第3波バックログ (improvements/backlog.md)](../improvements/backlog.md)
- [リアル購入処理の仕様 (specifications/features/real-trading-gmo-order.md)](../specifications/features/real-trading-gmo-order.md)

## この計画の背景

Claude Code / Codex / Antigravity の3ツールで現状を評価し、結果を突き合わせて作成しました（2026-08-29）。[改善計画](../improvements/README.md) の第1波・第2波（PR01〜PR10）が完了した時点の状態が出発点です。

3ツールの結論は一致しました。**現在のコードのまま実注文を有効にしてはいけません。**

## 最重要: いま実注文を有効にすると何が起きるか

[RealTradingService](../../projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/realtrading/RealTradingService.kt) は買い注文だけを送り、**売り注文は送りません**（`SELL_CANDIDATE` はログを出すだけです）。

1. 買い注文が約定して BTC を保有する。
2. 利確・損切りの判定が出ても、取引所には何も送られない。
3. **損切りが一度も発動しないまま、相場が下げ続ける限り含み損を抱える。**
4. 同時に「保有中」の状態で固定され、新規注文も出なくなる。

`max_order_jpy` などの上限は「1回に賭ける金額」を制限するだけで、**すでに持っているポジションの下落は止めません**。これは「自動売買」ではなく「出口のないポジションを作る処理」です。

## 計画の一覧

| 順序 | 計画 | ゴール | 実資金 |
| --- | --- | --- | --- |
| 1 | [PLAN00: フェーズの定義と安全契約を確定する](plan00-phase-and-safety-contract.md) ✅ | どこまで行けば実資金を入れてよいかを数字で決める | 動かさない |
| 2 | [PLAN01: 売り注文（SELL）を実注文で自動化する](plan01-real-sell-order.md) ✅ | 買い→保有→売り→現金 の1サイクルが閉じる | 動かさない |
| 3 | [PLAN02: 実注文の前に必要な安全ガードを揃える](plan02-order-safety-guards.md) ✅ | 誤発注・二重発注・上限超過を止める | 動かさない |
| 4 | [PLAN03: 通知を実装する（Phase2a）](plan03-notification.md) | 異常に人間が気づけて、危ないときは自動で止まる | 動かさない |
| 5 | [PLAN04: 本番へ配線し、実注文なしでリハーサルする](plan04-production-wiring-and-rehearsal.md) | 本番環境と障害時の挙動を確認する | 動かさない |
| 6 | [PLAN05: 最小額で実資金の1サイクルを通す（Phase3）](plan05-canary-with-real-money.md) | 監視下で実注文が1サイクル完結する | **動かす（最小額）** |
| 7 | [PLAN06: 無人の自動売買に昇格する（Phase4）](plan06-unattended-trading.md) | 承認なしで自動売買が回り続ける | **動かす** |

PLAN01 と PLAN02 は互いに独立なので、並行して進められます。PLAN03 も PLAN00 が終わっていれば並行できます。**PLAN05 だけは、PLAN00〜PLAN04 がすべて終わるまで着手できません。**

## なぜこの順序なのか

- **PLAN00 が先**: 現在の [ロードマップ](../overview/roadmap.md) は Phase3 の禁止事項として「手動の承認なしで実際の注文を出すこと」を挙げ、着手条件として Phase2（Web画面を含む）の完了を求めています。この矛盾を先に解かないと、以降の計画の完了条件が決められません。
- **PLAN01 が2番目**: 売れない状態でどれだけ安全ガードを積んでも、ポジションの下落は止まりません。ここが唯一「これが無いと絶対にダメ」な項目です。
- **PLAN02 が3番目**: 売買サイクルが閉じてから、誤発注・二重発注の経路を潰します。
- **PLAN03 が4番目**: 実資金を入れる前に、異常を人間が知る手段と、システムが自分で止まる条件を用意します。**手動承認を置かない方針（下記）なので、ここが人間側の唯一の歯止めです。**
- **PLAN04 が5番目**: 1円も動かさずに本番配線と異常系を確認します。
- **PLAN05 / PLAN06**: ここで初めてお金が動きます。最小額から、監視下で。

## 決定事項

**手動承認は実装しません。無人で運転し、安全は「金額の上限」と「システムが自分で止まること」で担保します。**（オーナー判断 / 2026-08-29）

[ロードマップ](../overview/roadmap.md) は PLAN00 で書き換え済みです。Phase2 は Phase2a（通知）と Phase2b（Web画面）に分割し、Phase3 の着手条件を Phase2a の完了に変えました。Phase3 の禁止事項から「手動の承認なしで実際の注文を出すこと」を外し、代わりに安全ルールを置いています。

承認を置かない代わりに、次の3つが**必須**です。どれも「後で入れる」にはできません。

1. 口座に入れる金額の上限（30,000円）
2. 自動で止まる条件（1日の損失上限 2,000円、3連敗、スリッページ 0.5%）— **まだ1つも実装されていません**
3. 通知と緊急停止

数値の正は [ロードマップ](../overview/roadmap.md) の「安全ルール（数値）」です。

## [backlog.md](../improvements/backlog.md) の8項目の扱い

| backlog番号 | 内容 | 扱い |
| --- | --- | --- |
| 1 | 市場データの妥当性検証 | [PLAN02](plan02-order-safety-guards.md)（実注文前に必須） |
| 2 | API リトライを指数バックオフ＋ステータス検証に統一 | [PLAN02](plan02-order-safety-guards.md)（実注文前に必須） |
| 3 | 重複実行を抑止する | [PLAN02](plan02-order-safety-guards.md)（実注文前に必須） |
| 4 | クールダウンの日跨ぎ fail-open | 実注文後でよい（ただし Cooldown 系 Strategy を実取引で許可する前には必須） |
| 5 | RealTradingSafetyChecker の入力値検証 | [PLAN02](plan02-order-safety-guards.md)（実注文前に必須） |
| 6 | ArchitectureTest 厳格化 / Clock 注入 | **項目を2つに割る**。Clock 注入は [PLAN02](plan02-order-safety-guards.md)（実注文前に必須）、依存方向の厳格化は実注文後でよい |
| 7 | gcloud / Terraform 一本化 | 実注文後でよい。ただしどちらを正とするかの宣言は [PLAN04](plan04-production-wiring-and-rehearsal.md) までに済ませる |
| 8 | 6時境界の判定スキップ | [PLAN02](plan02-order-safety-guards.md)（実注文前に必須） |

「不要」と判断した項目はありません。

## backlog に無かった、新たに見つかった問題

3ツールの評価で新しく出た項目です。すべて [PLAN01](plan01-real-sell-order.md) または [PLAN02](plan02-order-safety-guards.md) に含めています。

1. **売り注文が実装されていない**（[PLAN01](plan01-real-sell-order.md)）— 最重要。
2. **約定の反映が買い専用**（[PLAN01](plan01-real-sell-order.md)）— 約定を確認すると無条件に `isHolding = true` にします。売りの約定でこれを通すと、売ったのに保有中になります。
3. **安全チェックが買い専用**（[PLAN01](plan01-real-sell-order.md)）— 「保有中なら注文しない」という条件なので、そのまま売りに使うと逆の挙動になります。
4. ~~**数量の刻み（`sizeStep`）に丸めていない**~~ — 解消済み（[PLAN02](plan02-order-safety-guards.md) の A）。GMOコインの BTC は最小注文数量・刻みとも 0.00001 です。
5. ~~**注文数量の計算にK線の終値を使っている**~~ — 解消済み（[PLAN02](plan02-order-safety-guards.md) の B）。Ticker の最新価格を使い、手数料も上限判定に含めるようにしました。
6. **発注の「意図」を送信前に保存していない**（[PLAN01](plan01-real-sell-order.md)）— POST 後・保存前に落ちると、取引所に注文があるのにアプリ側に記録が残りません。
7. **通知が1つも実装されていない**（[PLAN03](plan03-notification.md)）— 停止しても誰も気づきません。
8. **損失上限・連敗停止・スリッページ上限が未実装**（[PLAN00](plan00-phase-and-safety-contract.md) で数字を決め、各計画で実装）— [ロードマップ](../overview/roadmap.md) の Phase3 完了条件「安全ルールが確実に動く」は未達です。
9. **同一口座の既存資産を巻き込む危険**（[PLAN01](plan01-real-sell-order.md)）— 取引所の残高をそのまま全量売ると、ボット以外が買った BTC まで売ります。専用口座を推奨します。
10. **緊急停止の手段が state.json の直接編集しかない**（[PLAN00](plan00-phase-and-safety-contract.md)）。

## 使い方

作業を依頼するときは、実施したい計画のファイルパスを指定してください。

```text
docs/plans/plan01-real-sell-order.md の内容を実施して
```

各計画は1つのPRには大きいので、着手時に分割してください（[pr-and-commit](../../.agents/skills/pr-and-commit/SKILL.md)）。分割の目安は各ファイルに書いてあります。

着手したら、上の一覧の「状態」を各ファイルで更新してください。
