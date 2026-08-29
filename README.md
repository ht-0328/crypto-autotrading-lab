# crypto-autotrading-lab

仮想通貨の自動売買ロジックを検証・運用するためのアプリケーションです。
最終目的は自動化による利益獲得ですが、初期段階（Phase1）は安全を優先し、実注文を行わないシミュレーション基盤として動作します。

## 主な機能と実行形態

本アプリケーションは以下の実行形態をサポートしています。

- **ローカルGradle実行**: `./gradlew run` による手軽な検証
- **Docker Compose実行**: `docker compose up` による独立環境での実行
- **GitHub Actions CI**: PR作成時等の自動テスト・検証
- **GCP Cloud Run Jobデプロイ**: GitHub Actions経由でGCP環境へデプロイしたシミュレーション運用

また、取引戦略には `Strategy` パターンが導入されており、以下の設定でロジックを切り替えることが可能です。

- **主な設定方法**:
  - 設定ファイル: `trading.strategy_name`
  - 環境変数: `APP_TRADING_STRATEGY_NAME`
- **使用できる戦略**:
  - `SafeReboundStrategy`: デフォルトの戦略。現在価格と買値（buyPrice）を基準に利確・損切りを判定します。
  - `CooldownReboundStrategy`: 損切り直後の再エントリーを一定期間制限（クールダウン）する戦略です（詳細は[こちら](docs/specifications/strategies/cooldown-rebound-strategy.md)）。
  - `TrendConfirmReboundStrategy`: 反発だけでなく、短期トレンド（MA5）の上向きを確認してからエントリーする戦略です（詳細は[こちら](docs/specifications/strategies/trend-confirm-rebound-strategy.md)）。
  - `AtrTrendConfirmReboundStrategy`: `TrendConfirmReboundStrategy` をベースにし、売り判定にATRを用いた変動幅を使用する戦略です（詳細は[こちら](docs/specifications/strategies/atr-trend-confirm-rebound-strategy.md)）。
  - `SimpleContrarianStrategy`: 比較用の旧ロジック（単純な逆張り）です。

## 開発ドキュメント

各種ドキュメント（人間向け・AI向け）は `docs/` 配下に整理されています。
詳細は以下のリンクから参照してください。

- [**ドキュメント一覧 (docs/README.md)**](docs/README.md)
- [ドキュメント一覧（全体像） (docs/overview/README.md)](docs/overview/README.md)
  - [売買ロジック説明 (docs/architecture/trading-logic.md)](docs/architecture/trading-logic.md)
- [AIエージェント向けルール (AGENTS.md)](AGENTS.md)

### 本プロジェクトの前提事項と注意事項

- **注意 (Phase1の制約):** このリポジトリでは現在Phase1を実行しており、実資金を用いた実注文は絶対に行いません。完全なシミュレーションとして動作します。
  - シミュレーション上の残金を `state.json` に保存します。
  - 買い判定時は残金を減らして保有BTC数量を更新し、売り判定時は残金を増やして確定損益を更新します。
  - CSVにも資金情報（残金、保有BTC数量、買値、確定損益）を出力します。
- **Strategyの切り替え:** アプリケーションは複数の取引ロジックを持っており、設定や環境変数（`APP_TRADING_STRATEGY_NAME`）によりStrategyの切り替えが可能です。
- **デプロイ手順:** GCP Cloud Run Job デプロイ手順は [docs/operations/gcp/README.md](docs/operations/gcp/README.md) を参照してください。
- **AIへの指示:** AIエージェントに作業させる場合のルールは [AGENTS.md](AGENTS.md) にまとまっています。Claude Code / Codex / Antigravity はいずれも自動で読み込みます（仕組みは [.agents/README.md](.agents/README.md) を参照）。

### 注文サイズの設定 (order_sizing_mode)

購入時の注文サイズ（金額）の決定方法として、`order_sizing_mode` を設定できます。
未指定時はデフォルトで `FIXED_AMOUNT` として動作します。

- **FIXED_AMOUNT**: 今まで通り `trade_amount` 円分だけ買うモードです。
- **ALL_IN**: 買う時に使える残高を全部使って買うモードです。（シミュレーションでは `cashBalance`、リアル取引では `JPY` 利用可能残高を使用します）

**注意:**
- `ALL_IN` の場合でも、既存設定との互換性や `FIXED_AMOUNT` モードの際に使われるため `trade_amount` の設定値は削除せずに残してください。
- リアル取引で `ALL_IN` を使う場合は、安全のため必ず `max_order_jpy` / `max_daily_order_jpy` / `max_position_jpy` の安全上限を設定・確認してください。

#### 設定例

**FIXED_AMOUNT の例:**
```yaml
trading:
  trade_amount: 10000
  order_sizing_mode: FIXED_AMOUNT
```

**ALL_IN の例:**
```yaml
trading:
  trade_amount: 10000
  order_sizing_mode: ALL_IN
```

## リポジトリ構成

主要ディレクトリの役割は以下です。

- `projects/crypto-autotrading-app/`: Kotlin CLI アプリ本体
- `config/`: 実行環境ごとの設定ファイル（GMO API / WireMock）
- `mocks/wiremock/`: WireMock のスタブ定義
- `docker/`: ローカル実行用の Dockerfile / Compose 定義
- `scripts/`: ローカル実行やクリーンアップ用のシェルスクリプト
- `docs/overview/`, `docs/architecture/`, `docs/development/`, `docs/operations/`: 人間向けの各種ドキュメント
- `AGENTS.md`, `.agents/`: AIエージェント向けの共通ルールとスキル（構成の説明は [.agents/README.md](.agents/README.md)）

## 起動方法 (ローカル実行)

ローカルでの実行確認用として以下のコマンドで起動できます。

```bash
cd projects/crypto-autotrading-app

# アプリの実行 (デフォルト設定)
./gradlew run

# テストの実行
./gradlew test
```

### DevContainer内でのメニュー実行

DevContainer環境では、以下のスクリプトを実行することで、メニュー形式で各種実行を行うことができます。
このスクリプトは、K線CSV取得・Private API残高確認・バックテストに必要な環境変数を自動設定して実行するためのものです。環境変数を毎回手入力せずにローカル実行を簡単に行えます。

```bash
./scripts/local/run-devcontainer-menu.sh
```

**メニューで選べる機能:**
- **リアルPublic APIでK線CSV取得**: 本物のGMO Public APIを利用してK線データをCSVとして取得します。
  - 実行時に取得期間の開始日・終了日を `yyyyMMdd` 形式（例: `20250101`）で入力する必要があります。指定した期間の範囲でK線CSVを取得します。
  - 未入力や `yyyyMMdd` 以外の形式を入力した場合はエラーになります。
- **リアルPrivate APIで残高確認**: 実際の口座の残高や状態を確認します（`GMO_API_KEY` と `GMO_API_SECRET` の環境変数設定が必要です）。
- **取得済みCSVでバックテスト**: 取得したCSVデータを使用してバックテストを実行します。
- **CSV取得 → 残高確認 → バックテストをまとめて実行**: 上記の一連の処理をまとめて実行します。
  - 実行時に取得期間の開始日・終了日の入力が同様に求められます。

**自動設定される主な環境変数:**
- **CSV取得用:** `KLINE_EXPORT_OUTPUT_PATH`, `KLINE_EXPORT_SYMBOL`, `KLINE_EXPORT_INTERVAL`, `KLINE_EXPORT_START_DATE`, `KLINE_EXPORT_END_DATE`
- **バックテスト用:** `BACKTEST_KLINE_CSV_PATH`, `BACKTEST_STRATEGY_NAME`, `BACKTEST_INITIAL_CAPITAL`, `BACKTEST_SUMMARY_OUTPUT_PATH`, `BACKTEST_STEPS_OUTPUT_PATH`

**設定ファイルの扱い:**
- 元の設定ファイル（`config/application-gmo.yaml`）は直接書き換えられません。
- 実行時に一時的な設定ファイルとして `data/local-devcontainer/application-runtime.yaml` が生成され、本物のAPI URL等の設定が反映されます。
- 実行時のデータ（CSVなど）は `data/local-devcontainer` ディレクトリに出力されます。

Docker Compose を使用する場合 (初回clone直後でもそのままビルド・起動できます):

```bash
docker compose -f docker/compose/local.yml up --build
```

### Docker リソースのクリーンアップ

`docker/compose/local.yml` で起動したコンテナ・ネットワーク・ボリュームと、Compose がビルドしたイメージを削除します。

```bash
# 削除対象を表示するだけ（削除しない）
./scripts/docker-clean.sh --dry-run

# 確認プロンプトを出したうえで削除する
./scripts/docker-clean.sh
```

- 既定ではこのリポジトリの Compose プロジェクトだけが対象です。他プロジェクトのコンテナやイメージ、DevContainer 自身は削除しません。
- `--all` を付けるとホスト上のすべての Docker リソースを削除します。**他プロジェクトのデータも消え、DevContainer 内で実行した場合は開発環境自体も削除対象になります。** 実行前に `DELETE-ALL` の入力を求められます。
