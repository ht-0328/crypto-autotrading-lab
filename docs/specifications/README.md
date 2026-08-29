# 仕様書 (Specifications)

| 項目 | 内容 |
| --- | --- |
| 想定読者 | 仕様を確認したい開発者 |
| 読んだあとできること | 目的の仕様書がどれかを選び、対応する設計書へ辿れる |
| 状態 | 現行 |
| 機密区分 | 公開可 |
| 作成者 | リポジトリ管理者 |
| 保守責任者 | リポジトリ管理者 |
| 最終確認日 | 2026-08-30 |


このディレクトリには、システムが何を満たす必要があるかを定義した「仕様書」が含まれています。
入力、出力、条件、エラー、具体例などを記載します。
どう作るか（設計）については、[`../architecture/`](../architecture/) を参照してください。

## 仕様書と設計書の違い

- **仕様書**: 何を満たす必要があるかを決める。クラス名やパッケージ構成は書かず、「入力」「出力」「ルール」に集中する。
- **設計書**: どう作るかを決める。責務分担、パッケージ構成、処理フローなどを記載する。

## 仕様書一覧

対象ごとに分けています。全体の前提を知りたい場合は Phase1 の仕様書から読んでください。

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
