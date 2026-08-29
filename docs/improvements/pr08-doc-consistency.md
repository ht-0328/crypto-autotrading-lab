# PR08: 仕様書・設計書の食い違いを解消する

| 項目 | 内容 |
| --- | --- |
| 想定読者 | この改善を実施する開発者、AIコーディングエージェント |
| 読んだあとできること | 仕様書・設計書と実装の食い違いを解消できる |
| 状態 | 実施済み（ブランチ `docs/spec-consistency`） |
| 機密区分 | 公開可 |
| 作成者 | リポジトリ管理者 |
| 保守責任者 | リポジトリ管理者 |
| 最終確認日 | 2026-08-30 |


## 対象の指摘

[findings.md](findings.md) の **V** / **W** / **X** / **Y**

## なぜ直すか

仕様書・設計書が実装とズレたままだと、新しく入った人（AIエージェント含む）が古い前提でコードを書きます。実際に次のズレがあります。

- **V**: 出力 CSV 名が仕様書 `history_YYYYMMDD.csv` / 実装 `trades_YYYYMMDD.csv`
- **W**: `order_sizing_mode`（`ALL_IN`）が仕様書に無く、「1回の売買金額 1,000円固定」という記述と矛盾する
- **X**: 朝6時境界で毎日 6:00〜約7:15 は判定がスキップされるが、どこにも書かれていない
- **Y**: 設計書の記述が実装と違う（Terraform の有無、`TradeDecision` の型、gcloud 依存の方針）。`isStopped=true` からの復旧手順が運用ドキュメントに無い

## 変更対象

このPRは**ドキュメントのみ**を変更します。

| ファイル | 変更内容 |
| --- | --- |
| [docs/specifications/phase1-simulation.md](../specifications/phase1-simulation.md) | CSV 名、ログ出力、`order_sizing_mode`、6時境界の挙動 |
| [docs/infrastructure/gcp/README.md](../infrastructure/gcp/README.md) | 「Terraform はまだ追加されていません」を現状に更新 |
| [docs/infrastructure/gcp/development-policy.md](../infrastructure/gcp/development-policy.md) | 現状は gcloud が正である旨を追記 |
| [docs/architecture/trading-strategy-design.md](../architecture/trading-strategy-design.md) | `enum TradeDecision` を data class に修正 |
| [docs/architecture/backtest-design.md](../architecture/backtest-design.md) | クラス名を実装と突き合わせる |
| `docs/operations/real-trading-recovery.md` | 新規作成。`isStopped` からの復旧手順 |
| [docs/operations/README.md](../operations/README.md) | 新規ファイルへのリンクを追加 |
| [docs/templates/](../templates/) | 文書に「状態」と「最終確認日」を付ける運用を追加 |

## 実施手順

上から順に実施してください。前の手順が終わってから次に進みます。

### 1. phase1-simulation.md の修正

- **53行付近（出力仕様）**: `data/history_20260501.csv` → `data/trades_20260501.csv`。実装は [CsvRepository.kt](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/infrastructure/output/CsvRepository.kt) で `trades.csv` → `trades_YYYYMMDD.csv` に変換している。
- **54行付近（出力仕様）**: ログ出力を「標準出力とファイル（`APP_DATA_DIR/app.log`）の両方」に修正する。[pr02-cloud-run-config.md](pr02-cloud-run-config.md) の実施後の状態に合わせること。
- **74行付近（判定条件・業務ルール）**: `order_sizing_mode` の節を追加する。

  | モード | 動作 | Phase1 での扱い |
  | --- | --- | --- |
  | `FIXED_AMOUNT` | `trade_amount` 円分だけ買う | 既定。Phase1 はこちらのみ推奨 |
  | `ALL_IN` | 使える残高を全部使って買う | バックテストでの検証用。実運用では使わない |

- **処理仕様（7章）**: 朝6時境界の既知の挙動を追加する。

  > K線の取得対象日は、取引所の営業日区切り（朝6時）に合わせて切り替わります（[TradingApplication.resolveKlineTargetDate()](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/application/TradingApplication.kt)）。そのため毎日 6:00 以降しばらくは判定に必要な本数（戦略により最大15本＝約75分）が揃わず、判定は「データ不足」としてスキップされます。保有中でも利確・損切りの判定は行われません。

### 2. 設計書の修正

- [infrastructure/gcp/README.md](../infrastructure/gcp/README.md) 9行付近の「現時点（Phase 1）では Terraform 実装ファイルはまだ追加されていません」を削除し、[infra/terraform/gcp/](https://github.com/ht-0328/crypto-autotrading-lab/tree/main/infra/terraform/gcp/) に実装があることを書く。
- [development-policy.md](../infrastructure/gcp/development-policy.md) 11行付近「手作業やGitHub Actions内の `gcloud` コマンドに依存しすぎない構成にする」に注記を足す。

  > **現状**: 構築とデプロイは GitHub Actions の `gcloud` コマンドが正です。Terraform コードは追加済みですが `terraform apply` は運用していません。一本化は今後の課題です。

- [trading-strategy-design.md](../architecture/trading-strategy-design.md) 23行付近の「enum `TradeDecision`」を修正する。実体は [TradeDecision.kt](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/model/TradeDecision.kt) の data class で、判定の種類は [TradeAction.kt](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/model/TradeAction.kt) の enum。配置も `domain.model` であり `domain.strategy` ではない。
- [backtest-design.md](../architecture/backtest-design.md) の Writer クラス名を [BacktestResultOutputPort.kt](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/repository/BacktestResultOutputPort.kt) など実装名に合わせる。

### 3. 復旧手順の新規作成

`docs/operations/real-trading-recovery.md` を作成します。設計書には「復旧は手動によるフラグリセット」とだけ書かれており、具体的な手順がどこにもありません。

含める内容:

- どういうときに `realTrading.isStopped` が `true` になるか（[RealTradingService.stopRealTrading()](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/realtrading/RealTradingService.kt) が呼ばれる条件）
- 停止理由の確認方法（`stopReason` / `stoppedAt` / `latestOrder`）
- GCS 上の `state.json` の取得方法（`gcloud storage cp`）
- 再開前に確認すること（取引所側の未約定注文の有無、実際の保有数量と `state.json` の一致）
- `isStopped` を `false` に戻して書き戻す手順
- 書き戻し後の確認方法

### 4. 文書の鮮度管理

[docs/templates/](../templates/) のテンプレートに、文書冒頭へ次を入れる欄を追加します。

- **状態**: 現行 / 将来案 / 廃止
- **最終確認日**: `YYYY-MM-DD`

[docs/templates/README.md](../templates/README.md) にも運用方法を1行追記します。

## 受け入れ条件

- [ ] 仕様書の CSV 名・ログ出力が実装と一致していること
- [ ] `order_sizing_mode` と 6時境界の挙動が仕様書に書かれていること
- [ ] 設計書に「Terraform は未追加」「`TradeDecision` は enum」といった誤った記述が残っていないこと
- [ ] `isStopped` からの復旧手順が [docs/operations/](../operations/) から辿れること
- [ ] テンプレートに「状態」と「最終確認日」の欄があること
- [ ] コードに変更が無いこと

## 検証

ドキュメントのみのため `./gradlew build` は不要です。次を確認します。

- 追加・変更したリンクがすべて有効であること（相対パス）
- [docs/README.md](../README.md) の「ドキュメントリンク方針」に沿っていること

## スコープ外

- 実注文の Phase 分離（[pr07-real-order-spec-separation.md](pr07-real-order-spec-separation.md)）
- バックテスト仕様の約定モデル変更（[pr06-backtest-execution-model.md](pr06-backtest-execution-model.md)）
- 6時境界の挙動そのものの改善（記載のみ。改善は [backlog.md](backlog.md)）
