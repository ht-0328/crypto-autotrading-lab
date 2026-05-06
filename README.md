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
  - `CooldownReboundStrategy`: 損切り直後の再エントリーを一定期間制限（クールダウン）する戦略です（詳細は[こちら](docs/architecture/cooldown-rebound-strategy.md)）。
  - `TrendConfirmReboundStrategy`: 反発だけでなく、短期トレンド（MA5）の上向きを確認してからエントリーする戦略です（詳細は[こちら](docs/architecture/trend-confirm-rebound-strategy.md)）。
  - `SimpleContrarianStrategy`: 比較用の旧ロジック（単純な逆張り）です。

## 開発ドキュメント

各種ドキュメント（人間向け・AI向け）は `docs/` 配下に整理されています。
詳細は以下のリンクから参照してください。

- [**ドキュメント一覧 (docs/README.md)**](docs/README.md)
- [ドキュメント一覧（全体像） (docs/overview/README.md)](docs/overview/README.md)
  - [売買ロジック説明 (docs/architecture/trading-logic.md)](docs/architecture/trading-logic.md)
- [AI向けドキュメント一覧 (docs/ai/README.md)](docs/ai/README.md)

### 本プロジェクトの前提事項と注意事項

- **注意 (Phase1の制約):** このリポジトリでは現在Phase1を実行しており、実資金を用いた実注文は絶対に行いません。完全なシミュレーションとして動作します。
  - シミュレーション上の残金を `state.json` に保存します。
  - 買い判定時は残金を減らして保有BTC数量を更新し、売り判定時は残金を増やして確定損益を更新します。
  - CSVにも資金情報（残金、保有BTC数量、買値、確定損益）を出力します。
- **Strategyの切り替え:** アプリケーションは複数の取引ロジックを持っており、設定や環境変数（`APP_TRADING_STRATEGY_NAME`）によりStrategyの切り替えが可能です。
- **デプロイ手順:** GCP Cloud Run Job デプロイ手順は [docs/operations/gcp/README.md](docs/operations/gcp/README.md) を参照してください。
- **AIへの指示:** AIエージェントに作業させる場合のルールは [AGENTS.md](AGENTS.md) を必ず参照させてください。

## リポジトリ構成

主要ディレクトリの役割は以下です。

- `projects/crypto-autotrading-app/`: Kotlin CLI アプリ本体
- `config/`: 実行環境ごとの設定ファイル（GMO API / WireMock）
- `mocks/wiremock/`: WireMock のスタブ定義
- `docker/`: ローカル実行用の Dockerfile / Compose 定義
- `docs/overview/`, `docs/architecture/`, `docs/development/`, `docs/operations/`: 人間向けの各種ドキュメント
- `docs/ai/`: AIエージェント向けのプロンプト・制約ルール

## 起動方法 (ローカル実行)

ローカルでの実行確認用として以下のコマンドで起動できます。

```bash
cd projects/crypto-autotrading-app

# アプリの実行 (デフォルト設定)
./gradlew run

# テストの実行
./gradlew test
```

Docker Compose を使用する場合 (初回clone直後でもそのままビルド・起動できます):

```bash
docker compose -f docker/compose/local.yml up --build
```
