# PLAN02: 実注文の前に必要な安全ガードを揃える

| 項目 | 内容 |
| --- | --- |
| 想定読者 | この計画を実施する開発者、AIコーディングエージェント |
| 読んだあとできること | 実注文の前に必要な安全ガードを実装できる |
| 状態 | 実施済み |
| 機密区分 | 公開可 |
| 作成者 | リポジトリ管理者 |
| 保守責任者 | リポジトリ管理者 |
| 最終確認日 | 2026-08-30 |


## なぜやるか

[PLAN01](plan01-real-sell-order.md) で売買サイクルが閉じても、それだけでは実資金を投入できません。次の経路が残るためです。

- 取引所の**最小注文数量**を満たさない注文を出し続け、毎回エラーで止まる（あるいは端数が残って買えなくなる）。
- 異常な価格データで誤った判定を出し、そのまま発注する。
- ジョブが重複起動して二重に発注する。
- 発注 POST がタイムアウトしたのに、取引所側では注文が通っている。
- 毎朝 6:00〜7:15 に判定が止まり、その間の損切りが発動しない。

## ゴール

「検知」「抑制」「停止」「復帰」の4観点で、実資金を投入しても致命傷にならない防御が揃っている。

## 含む作業

この計画で実施する作業です。作業ごとに分けてあり、個別に着手できます。

### A. 最小注文数量・数量刻み・ダストへの対応（新規。backlog には未記載）— 実施済み

**実施済み。** 以下は着手前の記録です。

[RealTradingService.calculateOrderSize()](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/realtrading/RealTradingService.kt) は次の計算をするだけです。

```kotlin
return BigDecimal(tradeAmount).divide(currentPrice, 8, RoundingMode.DOWN)
```

GMOコインの `GET /public/v1/symbols` で確認した BTC の制約は次です（2026-08-29 時点。**着手時に再確認してください**）。

| 項目 | 値 |
| --- | --- |
| `minOrderSize`（最小注文数量） | 0.00001 BTC |
| `sizeStep`（数量の刻み） | 0.00001 BTC |
| `takerFee`（成行の手数料） | 0.0005（0.05%） |

**問題は「最小注文数量」ではなく「数量の刻み」です。** BTC が 1,244万円のとき最小注文数量は約124円相当なので、`trade_amount: 1000` は最小値を上回っています。しかし刻みには合っていません。

```text
1000 ÷ 12,447,381 = 0.00008033...
小数8桁で切り捨て       → 0.00008033   ← 0.00001 の倍数ではないので取引所に拒否される
刻みに丸める            → 0.00008      ← 正しい（約996円）
```

拒否されると [RealTradingService](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/realtrading/RealTradingService.kt) の例外処理で `isStopped=true` になり、実注文が止まります。**金額を上げても解決しません。刻みへの丸めが必要です。**
- 売却時に手数料や丸めで**端数（ダスト）**が残ると、[RealTradingSafetyChecker](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/realtrading/RealTradingSafetyChecker.kt) の `currentHoldingAssets.isNotEmpty()` に永久に引っかかり、以後1回も買えなくなります。

やること:

1. ~~最小注文数量・数量刻みを設定値として持ち、注文数量をその刻みに切り捨てで丸める。売り注文の数量も同様に丸める。~~ 実施済み（`OrderSizeSpec`）
2. ~~丸めた結果が最小注文数量を下回るなら、**発注せず見送る**（例外にして停止させない。見送りは正常系）。~~ 実施済み
3. ~~「保有中とみなす閾値」を最小注文数量基準にし、ダストを保有と誤認しないようにする。~~ 実施済み
4. ~~`min_order_size` / `size_step` が未設定なら、**起動時に落とす**~~ 実施済み（[PR10](../improvements/pr10-config-fail-fast.md) の fail-fast と同じ方針）
5. ~~手数料（成行は 0.05%）を注文金額の上限判定に含める。~~ 実施済み（B とあわせて実施）

### B. 注文価格の基準と、成行注文のスリッページ上限（新規。backlog には未記載）— 実施済み

**注文数量の計算に使う「現在価格」が K線の終値です。** [TradingApplication](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/application/TradingApplication.kt) は Ticker を取得してログに出すだけで、`currentPrice` には最新K線の `close` を使っています。5分足の終値は最大で5分前の価格なので、急騰していると `tradeAmount / currentPrice` が実際より多い数量になり、**約定金額が `max_order_jpy` を超えます**。上限が上限として機能していません。

発注は `executionType = "MARKET"` の成行なので、板が薄いときや急変時にも想定と違う価格で約定します。

やること:

1. ~~注文数量の計算には Ticker の最新価格を使い、K線終値との乖離が大きいときは発注を見送る。~~ 実施済み
2. ~~手数料を含めた金額で上限を判定する。~~ 実施済み（`OrderPriceSpec`）
3. ~~約定価格が発注時の想定から一定率以上乖離していたら、次回以降の新規買いを止める。~~ 実施済み。**通知は [PLAN03](plan03-notification.md) で追加します。**
4. 指値に切り替えるかを判断する。指値は約定しないリスクがあるため、**損切りの売りは成行のまま**にするなどの整理が要ります。**未実施。当面は成行のままとします。**

### C. [backlog.md](../improvements/backlog.md) のうち実注文前に必須の項目

| backlog番号 | 内容 | なぜ実注文前に必須か |
| --- | --- | --- |
| ~~1~~ | ~~市場データの妥当性検証を Strategy の前段に置く~~ 実施済み | 異常値・古いデータでの誤発注を止めるため |
| ~~2~~ | ~~API リトライを指数バックオフ＋HTTPステータス検証に統一する~~ 実施済み | 発注 POST の不用意なリトライは二重注文に直結するため |
| ~~3~~ | ~~重複実行を抑止する~~ 実施済み（ロックファイル方式） | ジョブ重複起動時に2つの実行が同時に発注するため |
| ~~5~~ | ~~RealTradingSafetyChecker の入力値検証を追加する~~ 実施済み | 安全境界が不正値を素通しするため |
| ~~6（Clock 注入のみ）~~ | ~~`Clock` / `TimeProvider` を注入する~~ 実施済み | 日次注文上限のリセットが日付境界で正しく動くことを、固定時刻でテストできないため |
| ~~8~~ | ~~6時境界で判定がスキップされる時間帯を減らす~~ 実施済み | 毎朝75分間、保有ポジションの損切りが動かないため |

!!! warning "Clock 注入だけを先に切り出すこと"

    backlog 6 は「依存方向の厳格化」と「Clock 注入」の2つが1項目になっています。**Clock 注入だけを実注文前に切り出してください。** 日次上限は実際のお金の上限なので、日付が変わったときの挙動をテストで固定できない状態は残せません。依存方向の厳格化は後回しで構いません。

**2 について特に重要**: 発注 POST がタイムアウトしたとき、取引所側では注文が成立している可能性があります。**POST は自動リトライしてはいけません。** 次回実行時に注文照会で照合してから判断する経路を用意してください。着手前の実装は、例外を捕まえて `isStopped=true` にするだけです。注文IDが記録されないため、取引所にポジションがあるのにアプリ側に記録が無い状態になり得ます。

### D. 実注文後に回してよい項目

| backlog番号 | 内容 | 判断 |
| --- | --- | --- |
| 4 | クールダウンの日跨ぎ fail-open | エントリー精度の問題。損失の直接原因ではない |
| 6（依存方向の厳格化のみ） | ArchitectureTest の依存方向を厳格化する | 保守性の改善で、資金の損失には直結しない |
| 7 | gcloud / Terraform 一本化 | 現行の gcloud デプロイが動く限り取引動作に影響しない |

## 受け入れ条件

**注文数量と金額**

- ~~最小注文数量を下回る注文は送信されず、正常系の見送りとしてログに残る。~~ 実施済み
- ~~`min_order_size` / `size_step` が未設定のとき、起動時に失敗する。~~ 実施済み
- ~~ダストが残っていても、それを「保有中」と誤認しない。~~ 実施済み
- ~~注文数量の計算に Ticker の最新価格を使い、K線終値との乖離が大きいときは発注しない。~~ 実施済み
- ~~想定約定金額が `max_order_jpy` を超えない（手数料を含む）。~~ 実施済み

**市場データと時刻**

- ~~異常な市場データ（欠損・古い・価格が非正・高安の矛盾）を検知して `SKIP` する。~~ 実施済み
- ~~日次注文上限のリセットが、固定時刻を注入したテストで日付境界を含めて検証されている。~~ 実施済み
- ~~GET は指数バックオフで再試行し、発注 POST は再試行しない。~~ 実施済み

**通信と重複実行**

- ~~同じ実行が重複しても、二重に発注・保存しない。~~ 実施済み（ただし GCS マウント上での原子性は保証されない。[backlog 3](../improvements/backlog.md) 参照）
- ~~6:00〜7:15 でも判定に必要な本数が揃う。~~ 実施済み

**テスト**

- 上記すべてに単体テストがある。

## 検証手順

```bash
cd projects/crypto-autotrading-app
./gradlew build
```

## 分割の目安

このファイルは1つのPRには大きすぎます。着手時に少なくとも次に割ってください（[pr-and-commit](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/.agents/skills/pr-and-commit/SKILL.md)）。

**注文まわり**

1. ~~A（注文数量の刻みとダスト）~~ 実施済み
2. ~~B（注文価格の基準とスリッページ）+ A の手数料考慮~~ 実施済み
3. ~~backlog 5（入力値検証）~~ 実施済み

**実行基盤まわり**

4. ~~backlog 6 のうち Clock 注入~~ 実施済み
5. ~~backlog 1（市場データ検証）~~ 実施済み
6. ~~backlog 2（リトライとタイムアウト）~~ 実施済み
7. ~~backlog 3（重複実行抑止）~~ 実施済み
8. ~~backlog 8（6時境界）~~ 実施済み
