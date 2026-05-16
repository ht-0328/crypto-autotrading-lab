# 仕様書 (Specifications)

このディレクトリには、システムが何を満たす必要があるかを定義した「仕様書」が含まれています。
入力、出力、条件、エラー、具体例などを記載します。
どう作るか（設計）については、[`../architecture/`](../architecture/) を参照してください。

## 仕様書と設計書の違い

- **仕様書**: 何を満たす必要があるかを決める。クラス名やパッケージ構成は極力書かず、「入力」「出力」「ルール」に集中する。
- **設計書**: どう作るかを決める。責務分担、パッケージ構成、処理フローなどを記載する。

## 仕様書一覧

### 全体・フェーズ
- [Phase1 シミュレーション 仕様書](phase1-simulation.md)

### 機能 (Features)
- [バックテスト機能 仕様書](features/backtest.md)
- [過去K線CSV作成機能 仕様書](features/kline-csv-export.md)
- [過去K線CSV読み込み機能 仕様書](features/kline-csv-import.md)
- [リアル購入処理（GMOコイン） 仕様書](features/real-trading-gmo-order.md)

### 戦略 (Strategies)
- [CooldownReboundStrategy 仕様書](strategies/cooldown-rebound-strategy.md)
- [TrendConfirmReboundStrategy 仕様書](strategies/trend-confirm-rebound-strategy.md)
- [AtrTrendConfirmReboundStrategy 仕様書](strategies/atr-trend-confirm-rebound-strategy.md)
