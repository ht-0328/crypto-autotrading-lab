# PR05: Phase1 で実注文を構造的に不可能にする

| 項目 | 内容 |
| --- | --- |
| 想定読者 | この改善を実施する開発者、AIコーディングエージェント |
| 読んだあとできること | Phase1 で実注文経路に入れない起動時ガードを実装できる |
| 状態 | 実施済み（ブランチ `fix/phase1-real-order-guard`） |
| 機密区分 | 公開可 |
| 作成者 | リポジトリ管理者 |
| 保守責任者 | リポジトリ管理者 |
| 最終確認日 | 2026-08-30 |


## 対象の指摘

[findings.md](findings.md) の **O** / **K** / **L** / **S**

## なぜ直すか

[roadmap.md](../overview/roadmap.md) の Phase1 禁止事項は「実際の注文を送ること」です。しかしそれを担保しているのは、変更可能な2つの Boolean だけです。

- **O**: [Main.kt](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/presentation/Main.kt) は、2つが揃うだけで Private API を構築します。
  設定ミスや新しいデプロイ経路の追加だけで、禁止事項を破れます。
- **K**: [ci.yml](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/.github/workflows/ci.yml) で `dry_run: false` を選んだ場合の問題です。
  [prepare-ci-config.sh](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/ci/prepare-ci-config.sh) が `real_trade_enabled: true` を生成します。
  Private API は WireMock 固定なので実発注は届きません。
  それでも、CI に実取引経路を有効化する選択肢があること自体が禁止事項と衝突します。
- **L**: 実取引モードで SELL 判定が出ると、`RealTradingService` はログを出すだけです。しかし `SimulationService` が仮想売却して `isHolding=false` にします。
  取引所には BTC が残ったまま state が「未保有」になり、以降の判断がすべて狂います。
- **S**: 注文 POST 後、コンソール・CSV 出力を経てから state を保存します。
  POST 後・保存前に落ちると orderId を失い、再発注につながります。

## 変更対象

| ファイル | 変更内容 |
| --- | --- |
| [AppSettings.kt](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/model/AppSettings.kt) | `phase` 設定を追加（既定 `1`） |
| [ConfigLoader.kt](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/infrastructure/config/ConfigLoader.kt) | `phase` の読み込みと環境変数 `APP_PHASE` の上書き |
| [Main.kt](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/presentation/Main.kt) | Phase1 で `real_trade_enabled=true` を検出したら異常終了 |
| [TradingApplication.kt](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/application/TradingApplication.kt) | SELL の状態不整合を修正。実注文時の保存順序を変更 |
| [.github/workflows/ci.yml](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/.github/workflows/ci.yml) | `dry_run` 入力を削除 |
| [ci/prepare-ci-config.sh](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/ci/prepare-ci-config.sh) | 常に `dry_run: true` / `real_trade_enabled: false` 固定 |
| [config/application-gmo.yaml](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/config/application-gmo.yaml) | `app.phase: 1` を明示 |
| 各テスト | 下記の受け入れ条件に対応するケース |

## 実施手順

上から順に実施してください。前の手順が終わってから次に進みます。

### 1. 起動時ガードを入れる

1. `AppSettings` に `phase: Int = 1` を追加する（YAML のキー名と一致するので `@JsonProperty` は不要）。
2. `ConfigLoader` で `APP_PHASE` による上書きを追加する。安全上の設定なので、値が数値として解釈できない場合は既定値に戻さず起動時例外にする（`ConfigLoader.resolvePhase()`）。
3. `Main.kt` で、Private API クライアントを構築する前に判定する。

   ```kotlin
   private const val REAL_TRADING_ALLOWED_PHASE = 3

   val isRealTradeActive = config.realTrading.realTradeEnabled && !config.realTrading.dryRun
   if (config.app.phase < REAL_TRADING_ALLOWED_PHASE && isRealTradeActive) {
       logger.error { "Phase${config.app.phase} では実注文を実行できません。..." }
       error("Phase${config.app.phase} で実注文が有効化されています")
   }
   ```

   実注文が許されるのは Phase3 からです。[roadmap.md](../overview/roadmap.md) で実注文が Phase3 のスコープだからです。Phase2 の禁止事項にも「自動で実際の注文を出すこと」があります。

4. `config/application-gmo.yaml` の `app` に `phase: 1` を明示する。

### 2. SELL の状態不整合を直す

[TradingApplication.kt](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/application/TradingApplication.kt) の 125 行付近。

```kotlin
// 変更前
val shouldBypassSimulationStateUpdate = isRealTradeActive && decision.action == TradeAction.BUY_CANDIDATE

// 変更後: 実取引モードでは BUY / SELL いずれもシミュレーション状態を更新しない
val shouldBypassSimulationStateUpdate = isRealTradeActive &&
    (decision.action == TradeAction.BUY_CANDIDATE || decision.action == TradeAction.SELL_CANDIDATE)
```

SELL 検知時は状態を維持したまま警告ログを出す。実売却は Phase3 で実装します。

### 3. 実注文時の保存順序を変える

実注文パスに限り、`executeOrderIfNeeded()` の直後に `stateRepository.save()` を呼ぶ。
そのあとコンソール・CSV 出力へ進む。最後の保存はそのまま残してよい（同じ内容の再保存になる）。

### 4. CI の実取引経路を塞ぐ

1. `ci.yml` の `workflow_dispatch.inputs.dry_run` を削除する。
2. `prepare-ci-config.sh` の `DRY_RUN` / `REAL_TRADE_ENABLED` の分岐を削除し、常に次を書き出す。

   ```yaml
   real_trading:
     dry_run: true
     real_trade_enabled: false
   ```

3. `INPUT_DRY_RUN` を渡している `ci.yml` の `env` からも削除する。

## 受け入れ条件

- [ ] Phase1 で実注文が有効な設定なら、Private API を構築せず異常終了すること
- [ ] Phase1 では `APP_TRADING_*` などの環境変数をどう操作しても実注文経路に入らないこと
- [ ] 実取引モードで SELL 判定が出たとき、`isHolding` と `holdingAmount` が変化しないこと
- [ ] 実注文パスで、注文受付直後に state が保存されること
- [ ] `ci.yml` に `dry_run` の入力が存在しないこと
- [ ] `prepare-ci-config.sh` が常に `real_trade_enabled: false` を書き出すこと

## 検証

```bash
cd projects/crypto-autotrading-app
./gradlew build
```

CI 設定生成の確認:

```bash
cd /workspace
INPUT_STRATEGY_NAME=SafeReboundStrategy INPUT_PUBLIC_API_SOURCE=wiremock ci/prepare-ci-config.sh
grep -A3 'real_trading' build/ci-config/application-ci-docker.yaml   # 常に false であること
```

## スコープ外

- 実注文の SELL 自体の実装（Phase3）
- Cloud Run の Secret / Private API 権限の削除。実注文を Phase3 で使う前提のため今回は残す（[backlog.md](backlog.md) で扱う）
- 仕様書側の記述整理（[PR07](pr07-real-order-spec-separation.md)）
