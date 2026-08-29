# 指摘一覧と根拠

## 文書の目的

- [改善計画 (README.md)](README.md) の各作業が、どの指摘に対応しているかを示す
- 指摘の根拠となるファイルと行を記録し、後から妥当性を再確認できるようにする

## 対象読者

開発メンバー、AIコーディングエージェント

## 関連ドキュメント

- [改善計画 (README.md)](README.md)
- [第3波バックログ (backlog.md)](backlog.md)

## 調査方法

2026-08-29 に、Claude Code / Codex / Antigravity の3ツールで同じリポジトリを読み取り専用でレビューし、結果を突き合わせました。

| ツール | 実行方法 |
| --- | --- |
| Claude Code | リポジトリを直接読み込み。他2ツールの指摘を1件ずつ裏取り |
| Antigravity CLI (`agy`) | ヘッドレスではファイル読み取り権限が自動拒否されるため、対象ファイルをプロンプトに同梱して `--mode plan` で実行 |
| codex CLI | `codex exec --sandbox read-only` で全体レビュー1回と論点特化レビュー1回 |

いずれも静的レビューです。この時点で `./gradlew build` は実行していません。

## 指摘一覧

★ = 3ツール合意。「対応」列は [改善計画 (README.md)](README.md) の作業ファイル。

| # | 領域 | 内容 | 根拠 | 重要度 | 対応 |
| --- | --- | --- | --- | --- | --- |
| ★A | インフラ | `API_BASE_URL` / `GMO_PRIVATE_API_BASE_URL` を渡しているが、アプリが読むのは `API_PUBLIC_BASE_URL` / `API_PRIVATE_BASE_URL`。環境変数が完全に無視され、コンテナは隠れたデフォルト（本物の GMO API）に繋ぐ | [deploy-gcp.yml:97](../../.github/workflows/deploy-gcp.yml), [local.yml:13-14](../../docker/compose/local.yml) vs [ConfigLoader.kt:128-129](../../projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/infrastructure/config/ConfigLoader.kt) | 高 | [pr02](pr02-cloud-run-config.md) |
| ★B | インフラ | Dockerfile が jar しかコピーせず、Cloud Run 側も `APP_CONFIG_PATH` 未設定のため常に `createDefaultConfig()` にフォールバック。`order_sizing_mode` は環境変数上書きが無く Cloud Run で変更不能 | [Dockerfile:14](../../docker/app/Dockerfile), [ConfigLoader.kt:160](../../projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/infrastructure/config/ConfigLoader.kt) | 高 | [pr02](pr02-cloud-run-config.md), [pr10](pr10-config-fail-fast.md) |
| ★C | インフラ | Cloud Run Job 定義が gcloud と Terraform の2箇所にあり環境変数も食い違う。`terraform apply` するワークフローは存在しない | [deploy-gcp.yml:109-124](../../.github/workflows/deploy-gcp.yml) vs [cloud-run-job.tf](../../infra/terraform/gcp/cloud-run-job.tf) | 中 | [pr10](pr10-config-fail-fast.md)（乖離解消のみ）、一本化は [backlog](backlog.md) |
| ★D | 実装 | `stop_on_unconfirmed_order` は読み込まれるだけでどの判定にも使われていない（SafetyChecker は常に停止する＝現状は安全側） | [RealTradingConfig.kt:20-21](../../projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/model/realtrading/RealTradingConfig.kt) vs [RealTradingSafetyChecker.kt:53-61](../../projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/realtrading/RealTradingSafetyChecker.kt) | 低 | [pr10](pr10-config-fail-fast.md) |
| ★E | 仕様 | Phase1 仕様書は「対象外: 実注文・Private API」、ロードマップ Phase1 禁止事項は「実際の注文を送ること」。しかし実注文機能は `placeOrder` まで実装済みで、Terraform は GMO APIキーの Secret も作る | [phase1-simulation.md:23-24](../specifications/phase1-simulation.md), [roadmap.md:45](../overview/roadmap.md) vs [RealTradingService.kt:349-355](../../projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/realtrading/RealTradingService.kt), [secrets.tf](../../infra/terraform/gcp/secrets.tf) | 高 | [pr07](pr07-real-order-spec-separation.md) |
| ★F | 実装 | `logback.xml` がファイル出力のみで Cloud Logging にアプリログが残らない。仕様書は「ログ出力: (標準出力)」 | [logback.xml](../../projects/crypto-autotrading-app/src/main/resources/logback.xml), [phase1-simulation.md:54](../specifications/phase1-simulation.md) | 高 | [pr02](pr02-cloud-run-config.md) |
| ★G | CI | `strategy_name` の選択肢が実装済み5戦略と食い違う（`ci.yml` は2件、他2つは `AtrTrendConfirmReboundStrategy` が欠落） | [ci.yml:28-29](../../.github/workflows/ci.yml), [deploy-gcp.yml:16-19](../../.github/workflows/deploy-gcp.yml), [backtest-smoke.yml:20-23](../../.github/workflows/backtest-smoke.yml) | 中 | [pr09](pr09-ci-compose-consistency.md) |
| ★H | インフラ | Terraform の Cloud Run Job に `APP_DATA_DIR` が無い。`output_path` / `state_path` は `/mnt/gcs/...` 絶対パスなので `state.json` と結果ファイルは残るが、`app.log` だけがコンテナローカル `/app/data` に出て Job 終了時に消える | [cloud-run-job.tf](../../infra/terraform/gcp/cloud-run-job.tf), [variables.tf:88-98](../../infra/terraform/gcp/variables.tf) | 中 | [pr02](pr02-cloud-run-config.md) |
| ★I | 実装 | `StateRepository.save()` が例外を握り潰して継続。保存に失敗しても正常終了（exit 0）扱いになり、仕様「エラーをログに記録し終了する」に反する | [StateRepository.kt:65-67](../../projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/infrastructure/output/StateRepository.kt), [phase1-simulation.md:63](../specifications/phase1-simulation.md) | 高 | [pr04](pr04-state-repository-crash-safe.md) |
| ★J | 実装 | `state.json` の書き込みが非アトミック（`writeText`）。途中で落ちると壊れ、次回 `load()` が例外を投げて以降の実行も止まる | [StateRepository.kt:62](../../projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/infrastructure/output/StateRepository.kt) | 高 | [pr04](pr04-state-repository-crash-safe.md) |
| ★K | CI | `ci.yml` の `dry_run: false` を選ぶと `prepare-ci-config.sh` が `real_trade_enabled: true` を生成する。Private API は WireMock 固定なので現状で実発注は届かないが、Phase1 の CI に実取引経路を有効化する選択肢が存在すること自体が禁止事項と衝突 | [ci.yml:9-14](../../.github/workflows/ci.yml), [prepare-ci-config.sh:36-49](../../ci/prepare-ci-config.sh) | 高 | [pr05](pr05-phase1-real-order-guard.md) |
| L | 実装 | 実取引モードの SELL でローカル保有状態が壊れる。`RealTradingService` は SELL でログを出すだけなのに、`shouldBypassSimulationStateUpdate` が BUY のみを対象にしているため `SimulationService` が仮想売却し `isHolding=false` になる。取引所には BTC が残ったまま state が「未保有」になり、以降の損切り・保有上限判断が全て狂う | [TradingApplication.kt:125](../../projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/application/TradingApplication.kt) vs [RealTradingService.kt:78-81](../../projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/realtrading/RealTradingService.kt) | 高（実注文有効時） | [pr05](pr05-phase1-real-order-guard.md) |
| M | 実装 | Private API のレスポンス本文を INFO ログにそのまま出力している。口座残高・注文情報が `app.log` に残る。[AGENTS.md](../../AGENTS.md) の「秘密情報を…ログにも出しません」に抵触 | [GmoPrivateApiClientImpl.kt](../../projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/infrastructure/exchange/gmo/GmoPrivateApiClientImpl.kt) の54, 76, 64, 89, 129, 150行ほか | 高 | [pr03](pr03-private-api-log-leak.md) |
| N | 開発体験 | `docker-clean.sh` がホスト上の全コンテナ・全イメージ・全ボリューム・全カスタムネットワーク・全ビルドキャッシュを確認なしで削除する。他プロジェクトのデータも消え、DevContainer 内から実行すれば自分自身も対象 | [docker-clean.sh](../../scripts/docker-clean.sh) | 高 | [pr01](pr01-docker-clean-scope.md) |
| O | 実装 | 実注文の禁止が変更可能な2つの Boolean だけに依存しており、Phase1 固有の強制ガードが無い。設定ミス・新しいデプロイ経路の追加だけで禁止事項を破れる | [Main.kt:62](../../projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/presentation/Main.kt) | 高 | [pr05](pr05-phase1-real-order-guard.md) |
| P | 仕様 | バックテストが同一足の終値で判定し、同じ終値で約定する（look-ahead）。手数料・スプレッド・スリッページも無い。全戦略の成績が構造的に楽観化しており、Phase1 の「戦略を検証する」という目的自体が損なわれている | [backtest.md:91](../specifications/features/backtest.md), [BacktestEngine.kt:59](../../projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/backtest/BacktestEngine.kt) | 高 | [pr06](pr06-backtest-execution-model.md) |
| Q | 仕様 | [product.md](../overview/product.md) が「安全設計」として宣言している3項目が未実装。(1) 市場データとの時刻ズレ ±60秒検知 → Ticker は取得してログに出すだけ、(2) 指数バックオフ → 固定1秒リトライ、(3) 重複実行時の二重更新スキップ → ロック無し | [product.md:63-72](../overview/product.md) vs [TradingApplication.kt:210-212](../../projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/application/TradingApplication.kt), [GmoPublicApiClient.kt:34-48](../../projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/infrastructure/exchange/gmo/GmoPublicApiClient.kt) | 高 | [backlog](backlog.md) |
| R | 実装 | クールダウン判定が日跨ぎで fail-open。`lastStopLossTime` が取得済み Kline 内に無いとクールダウン解除扱いになるため、前日終盤の損切り後に日付が変わると予定より早く再エントリーできる | [CooldownReboundStrategy.kt:94](../../projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/strategy/CooldownReboundStrategy.kt), [TrendConfirmReboundStrategy.kt:89](../../projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/strategy/TrendConfirmReboundStrategy.kt) | 中 | [backlog](backlog.md) |
| S | 実装 | 注文 POST 後、コンソール・CSV 出力を経てから最後に state を保存する。POST 後・保存前に落ちると orderId を失い再発注につながる | [TradingApplication.kt:99-193](../../projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/application/TradingApplication.kt) | 中（実注文有効時） | [pr05](pr05-phase1-real-order-guard.md) |
| T | 実装 | 環境変数のパース失敗（`toIntOrNull()` 等）が黙ってベース値にフォールバックし、設定ミスに気付けない | [ConfigLoader.kt:150-175](../../projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/infrastructure/config/ConfigLoader.kt) | 中 | [pr10](pr10-config-fail-fast.md) |
| U | インフラ | `docker compose up`（README の推奨手順）が、1回実行して終了するバッチに `restart: unless-stopped` を付けているため無限再起動し、設定は本物の GMO Public API を向いている。WireMock は起動するだけで Public API には使われない。`version: "3.8"` は非推奨、`wiremock:latest` は CI の `3.5.2` と非固定 | [local.yml:1-31](../../docker/compose/local.yml) | 高 | [pr09](pr09-ci-compose-consistency.md) |
| V | 仕様 | CSV 名が仕様書 `history_YYYYMMDD.csv` / 実装 `trades_YYYYMMDD.csv` で食い違う | [phase1-simulation.md:53](../specifications/phase1-simulation.md) vs [CsvRepository.kt:38-45](../../projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/infrastructure/output/CsvRepository.kt) | 低 | [pr08](pr08-doc-consistency.md) |
| W | 仕様 | `order_sizing_mode`（`ALL_IN`）が仕様書に無く、Phase1 の「1回1,000円固定」という記述と矛盾する | [phase1-simulation.md:74](../specifications/phase1-simulation.md) | 中 | [pr08](pr08-doc-consistency.md) |
| X | 仕様 | 朝6時境界で K線が不足し、毎日 6:00〜約7:15 は判定がスキップされ、保有中でも利確・損切りが働かない（例外は起きない。各 Strategy にガードあり）。仕様書に記載が無い | [TradingApplication.kt:236-243](../../projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/application/TradingApplication.kt) | 中 | [pr08](pr08-doc-consistency.md) |
| Y | ドキュメント | 設計書が実装とズレている。[infrastructure/gcp/README.md:9](../infrastructure/gcp/README.md)「Terraform 実装ファイルはまだ追加されていません」（実際は追加済み）、[trading-strategy-design.md:23](../architecture/trading-strategy-design.md)「enum `TradeDecision`」（実体は data class）、[development-policy.md:11](../infrastructure/gcp/development-policy.md)「gcloud に依存しすぎない」（実態は gcloud が正）。`isStopped=true` からの復旧手順が [運用ドキュメント](../operations/) に無い | 上記各所 | 中 | [pr08](pr08-doc-consistency.md) |
| Z | 開発体験 | 設定の切り替えが sed による YAML 書き換えで2箇所に散在。`ConfigLoader` は環境変数上書きに対応済みなので sed は不要 | [prepare-ci-config.sh:22-33](../../ci/prepare-ci-config.sh), [run-devcontainer-menu.sh:29-37](../../scripts/local/run-devcontainer-menu.sh) | 低 | [pr10](pr10-config-fail-fast.md) |
| AA | CI | `./gradlew build`（テスト込み）の直後に `./gradlew test --tests "*Architecture*"` を再実行していて二重 | [ci.yml:47-56](../../.github/workflows/ci.yml) | 低 | [pr09](pr09-ci-compose-consistency.md) |
| AB | 実装 | `RealTradingSafetyChecker` が `tradeAmount <= 0` / `currentPrice <= 0` / 上限値0以下といった不正値を明示的に拒否していない（現状は下流で偶然弾かれる） | [RealTradingSafetyChecker.kt:33-40](../../projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/realtrading/RealTradingSafetyChecker.kt) | 中 | [backlog](backlog.md) |
| AC | 実装 | API 境界で HTTP ステータスを検証せずデコードしている。`ArchitectureTest` は application ↔ infrastructure の相互依存を許可、domain が現在時刻に直接依存（`Clock` 未注入）で日付境界のテストが書けない | [GmoPublicApiClient.kt:63](../../projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/infrastructure/exchange/gmo/GmoPublicApiClient.kt), [ArchitectureTest.kt:31-40](../../projects/crypto-autotrading-app/src/test/kotlin/cryptoautotrading/architecture/ArchitectureTest.kt) | 中 | [backlog](backlog.md) |

## 誤りだった指摘

レビュー中に挙がったが、裏取りの結果そのままでは成り立たなかったものです。同じ指摘を繰り返さないために残します。

| 内容 | 裏取り結果 |
| --- | --- |
| 朝6時境界で K線が不足し、ATR や MA5 の計算で例外が発生する | 誤り。各 Strategy に「データ不足」ガードがあり例外は起きない。ただし判定がスキップされ続ける問題は実在するため、指摘 X として記録した |
| Terraform の `APP_DATA_DIR` 欠落により `state.json` と CSV が消える | 半分誤り。`output_path` / `state_path` は `/mnt/gcs/...` の絶対パスなので残る。消えるのは `app.log` だけ（指摘 H として記録） |
| `ci.yml` で `dry_run: false` を選ぶと実発注が飛ぶ | 現状では起きない。`private_base_url` が常に WireMock 固定で `DummyGmoCredentialProvider` が使われる。ただし将来の踏み外しを招くため指摘 K として記録した |
| 実注文前に GMO の JPY 残高を確認していない | 誤り。`RealTradingService.checkJpyBalance()` で確認している |
