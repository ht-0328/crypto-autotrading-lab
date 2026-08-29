# 設計 (Architecture)

| 項目 | 内容 |
| --- | --- |
| 想定読者 | 設計書を探している開発者 |
| 読んだあとできること | 目的の設計書がどれかを選び、対応する仕様書へ辿れる |
| 状態 | 現行 |
| 機密区分 | 公開可 |
| 作成者 | リポジトリ管理者 |
| 保守責任者 | リポジトリ管理者 |
| 最終確認日 | 2026-08-30 |


このディレクトリには、システムをどのように実装するかを定義した「設計書」が含まれています。
仕様書（何を満たすべきか）については、[`../specifications/`](../specifications/) を参照してください。

## 設計書一覧

- [システム全体構成 設計書](system-overview.md): Phase1におけるシステム全体の構成、パッケージ設計、実行フロー。
  - 対応する仕様書: [Phase1 仕様書](../specifications/phase1-simulation.md)
- [売買戦略の基本ロジック](trading-logic.md): 各戦略が共通して利用する基本的な売買ロジックの考え方。
- [売買戦略（Trading Strategy） 設計書](trading-strategy-design.md): 各種戦略（Strategyパターン）の実装方針と状態管理。
- [過去K線CSV作成機能 設計書](kline-csv-export-design.md): GMO Public API からのK線取得とCSV保存の設計。
  - 対応する仕様書: [過去K線CSV作成機能 仕様書](../specifications/features/kline-csv-export.md)
- [過去K線CSV読み込み機能 設計書](kline-csv-import-design.md): 過去K線CSVの読み込み処理の設計。
  - 対応する仕様書: [過去K線CSV読み込み機能 仕様書](../specifications/features/kline-csv-import.md)
- [バックテスト機能 設計書](backtest-design.md): バックテストの処理フローと計算設計。
  - 対応する仕様書: [バックテスト機能 仕様書](../specifications/features/backtest.md)
- [リアル購入処理（GMOコイン） 設計書](real-trading-gmo-order-design.md): GMO Private API を利用したリアル注文処理の設計。
  - 対応する仕様書: [リアル購入処理（GMOコイン） 仕様書](../specifications/features/real-trading-gmo-order.md)
- [リアル購入処理 詳細設計](real-trading-gmo-order-detailed-design.md): DTOや詳細なAPI連携の設計。
