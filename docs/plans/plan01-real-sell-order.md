# PLAN01: 売り注文（SELL）を実注文で自動化する

| 項目 | 内容 |
| --- | --- |
| 想定読者 | この計画を実施する開発者、AIコーディングエージェント |
| 読んだあとできること | 売り注文を実注文で自動化できる |
| 状態 | 実施済み（ブランチ `feat/real-sell-order`）。ただし下記「積み残し」は別PRに分離 |
| 機密区分 | 公開可 |
| 作成者 | リポジトリ管理者 |
| 保守責任者 | リポジトリ管理者 |
| 最終確認日 | 2026-08-30 |


## なぜやるか

**これが終わるまで、実注文は絶対に有効化できません。** 現在の実装は「買うが、売れない」状態です。

- [RealTradingService.executeOrderIfNeeded()](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/realtrading/RealTradingService.kt) は `SELL_CANDIDATE` を受け取ってもログを1行出すだけで、取引所には何も送りません。
- [TradingApplication](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/application/TradingApplication.kt) は実取引モードのとき、SELL でシミュレーション状態を更新しないよう意図的にバイパスしています（[PR05](../improvements/pr05-phase1-real-order-guard.md) で入れた安全措置）。つまり保有状態は「保有中」のまま固定されます。
- 結果として、この状態で実注文を有効にすると次が起きます。
  1. 買い注文だけが約定し、BTC を保有する。
  2. Strategy が利確・損切りの `SELL_CANDIDATE` を出しても、注文は送られない。
  3. **プログラムによる損切りが一度も発動せず、相場が下げ続ける限り含み損を抱え続ける。**
  4. 同時に `isHolding=true` のまま固定されるため、安全チェック（保有中は買わない）に永久に引っかかり、新規注文も出なくなる。

`max_order_jpy` などの上限は「1回に賭ける金額」を制限するだけで、**すでに持っているポジションの下落は一切止めません**。上限設定はこのリスクの代替になりません。

## ゴール

「買い → 保有 → 売り（利確・損切り） → 現金に戻る」の1サイクルが、人手を介さず取引所側で完結する。

## 変更対象

| ファイル | 変更内容 |
| --- | --- |
| [RealTradingService.kt](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/realtrading/RealTradingService.kt) | `SELL_CANDIDATE` の実注文処理を追加。約定反映を売買区分で分岐 |
| [RealTradingSafetyChecker.kt](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/realtrading/RealTradingSafetyChecker.kt) | 売り注文用の安全チェックを追加 |
| [TradingApplication.kt](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/application/TradingApplication.kt) | SELL のバイパス条件を、約定確認済みなら状態を進める形に見直す |
| [real-trading-gmo-order.md](../specifications/features/real-trading-gmo-order.md) | 「売り注文（SELL）の自動化」を対象外から対象へ移す |
| [real-trading-gmo-order-design.md](../architecture/real-trading-gmo-order-design.md) / [同 詳細設計](../architecture/real-trading-gmo-order-detailed-design.md) | 売り注文のフローを追記 |
| 各テスト | 下記の受け入れ条件に対応するケース |

## 設計上の論点（着手前に決めること）

実装に入る前に決めておく論点です。決めずに書き始めると手戻りになります。

### 1. 約定反映が買い専用になっている

[RealTradingService.handleExecutedOrder()](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/realtrading/RealTradingService.kt) は約定を確認すると無条件に次を返します。

```kotlin
return currentState.copy(
    isHolding = true,
    buyPrice = averagePrice,
    holdingAmount = summary.totalSize,
    ...
)
```

売り注文の約定でこれを通すと、**売ったのに「保有中」になります**。`latestOrder.side` で分岐し、SELL の約定では `isHolding=false` / `holdingAmount=0` / `buyPrice=0` にし、`realizedProfitAndLoss` に確定損益を加算してください。

### 2. 安全チェックが買い専用になっている

[RealTradingSafetyChecker.checkPreOrderSafety()](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/realtrading/RealTradingSafetyChecker.kt) は `state.isHolding || currentHoldingAssets.isNotEmpty()` で注文を止めます。これは二重買いを防ぐための条件なので、**そのまま売りに使うと、保有しているときだけ売れないという逆の挙動になります**。売り用のチェックを別に用意してください。売りで確認すべきことは次です。

- 取引所側の実残高が、売ろうとしている数量以上あること（`state` ではなく取引所の値を正とする）
- 未約定注文が無いこと
- 売却数量が取引所の最小注文数量・数量刻みを満たすこと（[PLAN02](plan02-order-safety-guards.md) と重なる）

### 3. `isStopped` のとき、損切りの売りも止めるべきか

現在の [復旧手順](../operations/real-trading-recovery.md) のとおり、例外が起きると `realTrading.isStopped=true` になり新規注文が止まります。ここで**売りまで止めると、ポジションを抱えたまま損切り不能になります**。

推奨: `isStopped` は**新規の買いだけを止め、保有解消の売りは通す**。ただし売りでも例外が起きた場合は通知して人間の判断を仰ぎます。これは「迷ったら止まる側」の原則に対する明示的な例外なので、[trading-safety-review](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/.agents/skills/trading-safety-review/SKILL.md) の観点でレビューし、決めた理由を仕様書に残してください。

### 4. 売る数量（取引所の残高をそのまま全量売ってはいけない）

部分約定や手数料により、`state.holdingAmount` と取引所の実残高はズレます。かといって**取引所の BTC 残高をそのまま全量売るのは危険です**。同じ口座に、このボット以外が買った BTC（手動購入、他の用途の資産）があれば、それも売り払ってしまいます。

決め方:

- 売却数量は **「このボットが約定で積み上げた保有数量」と「取引所の実残高」の小さい方**にする。ボットの記録より実残高が少ないときは、外部要因で減っているので通知して止める。
- あわせて、**このボット専用の取引口座（またはサブ口座）を用意することを強く推奨**します。口座を分けられるなら、上記の複雑さの多くが消えます。運用方針として決めて記録してください。
- 端数（ダスト）の扱いは [PLAN02](plan02-order-safety-guards.md) で扱います。

### 5. 発注の「意図」を送信前に保存する

着手前の実装は、注文が受け付けられてから `orderId` を保存します（[buildOrderedState()](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/realtrading/RealTradingService.kt)）。POST 送信後・保存前に落ちると、取引所には注文があるのにアプリ側に記録が無い状態になります。

**発注する直前に「これから何を注文するか」を state に書いてから POST する**（意図の先行保存）ようにしてください。次回起動時に、意図が残っていて結果が未確認なら、注文照会で照合してから次に進みます。これは SELL だけの話ではなく BUY にも必要ですが、SELL の実装で注文の状態遷移に手を入れるこの計画でまとめて直すのが自然です。

## 実施手順

1. `RealOrderState.side` を使って約定反映を BUY / SELL で分岐させる。
2. 売り用の安全チェックを `RealTradingSafetyChecker` に追加する。
3. 発注意図の先行保存を BUY / SELL 共通で入れる。
4. `executeSellCandidateOrder()` を実装する（取引所残高の取得 → 売却数量の決定 → 安全チェック → 意図を保存 → 成行売り → 注文IDを state に保存）。
5. `isStopped` 時の売りの扱いを決めて実装する。
6. `TradingApplication` の SELL バイパス条件を見直す。約定が確認できた売りは、シミュレーション状態にも反映されるようにする。
7. 仕様書・設計書を更新する。専用口座の方針も記録する。

## 受け入れ条件

- 実取引モードで `SELL_CANDIDATE` が出たとき、取引所へ売り注文が送信される。
- 売り注文の約定を確認したあと、`isHolding=false` / `holdingAmount=0` になり、確定損益が加算される。
- 約定が確認できないうちは保有状態を変えない（未確認注文として次回に持ち越す）。
- 取引所の実残高が0のときに売り注文を送らない。
- 売却数量が「ボットの記録した保有数量」を超えない。実残高が記録より少ないときは発注せず通知する。
- 発注 POST の直前に意図が保存されており、POST 後・結果保存前に落ちても、次回起動時に注文照会で照合してから進む（二重発注しない）。
- `isStopped=true` のとき、決めたとおりの挙動になる（買いは止まる／売りの扱いは決定に従う）。
- 上記すべてに単体テストがある。

## 検証手順

```bash
cd projects/crypto-autotrading-app
./gradlew build
```

加えて、WireMock を使った結合確認で「買い → 約定 → 保有 → 売り → 約定 → 未保有」の一巡が通ることを確認します。売り用のスタブは [mocks/wiremock/](https://github.com/ht-0328/crypto-autotrading-lab/tree/main/mocks/wiremock/) に追加が必要です。

## 積み残し（別PRに分離）

論点5の**発注意図の先行保存は、このPRには含めていません。** 注文の送信前に状態を保存するには、ドメインサービスから状態リポジトリを呼ぶ必要があります。売り注文の実装とは別のPRになるためです（[pr-and-commit](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/.agents/skills/pr-and-commit/SKILL.md) の1PR1変更）。**[PLAN05](plan05-canary-with-real-money.md) の着手条件には含まれるので、実資金を入れる前に必ず実施してください。**

実装後に判明した制約も、[PLAN02](plan02-order-safety-guards.md) までに解消が必要です。

- 確定損益に**買い時の手数料が反映されません**。売り時の手数料のみ差し引いています。
- 実注文の売りでは `lastStopLossTime` を更新しないため、**クールダウンを使う Strategy を実取引で使えません**。`CooldownReboundStrategy` / `TrendConfirmReboundStrategy` / `AtrTrendConfirmReboundStrategy` が該当します。実取引は `SafeReboundStrategy` に限定してください（[PLAN00](plan00-phase-and-safety-contract.md) の Strategy allowlist）。

## やらないこと

- 指値注文、分割売却、トレーリングストップ。まず成行の全量売却で1サイクルを閉じます。
- 通知（[PLAN03](plan03-notification.md) で扱います）。
- Phase ガードの解除（[PLAN04](plan04-production-wiring-and-rehearsal.md) で扱います）。
