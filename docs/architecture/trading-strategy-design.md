# 売買戦略（Trading Strategy） 設計書

| 項目 | 内容 |
| --- | --- |
| 想定読者 | 売買戦略を追加・変更する開発者 |
| 読んだあとできること | Strategy を1つ追加するために何を実装すればよいかを判断できる |
| 状態 | 現行 |
| 機密区分 | 公開可 |
| 作成者 | リポジトリ管理者 |
| 保守責任者 | リポジトリ管理者 |
| 最終確認日 | 2026-08-30 |


## 1. 文書の目的

この文書では、各種売買戦略の実装方針を定義する。Strategy パターンの使い方と `SimulationService` との連携を扱う。

## 2. 対応する仕様書

- [CooldownReboundStrategy 仕様書](../specifications/strategies/cooldown-rebound-strategy.md)
- [TrendConfirmReboundStrategy 仕様書](../specifications/strategies/trend-confirm-rebound-strategy.md)
- [AtrTrendConfirmReboundStrategy 仕様書](../specifications/strategies/atr-trend-confirm-rebound-strategy.md)

## 3. 設計方針

- **Strategyパターン**: 各戦略は `TradingStrategy` を実装する。呼び出し側は具体的なロジックを意識せず切り替えられる。
- **状態を持たない**: `TradingStrategy` のインスタンスには状態を持たせない。必要な状態は引数（`SimulationState`等）として毎回渡し、更新後の状態を返すか、呼び出し側に委ねる。
- **責務の分離**: Strategy は判定だけを行う。残高やポジションの更新は別のサービスが行う。

## 4. 責務分担

| 層・部品 | 役割 |
| --- | --- |
| interface `TradingStrategy` | 全ての戦略が実装すべきメソッド（`judge()`）を定義する |
| class 各種Strategy | 渡されたK線データと状態をもとに `TradeDecision` を返す |
| enum `TradeAction` | 判定の種類（`BUY_CANDIDATE`, `SELL_CANDIDATE`, `HOLDING`, `SKIP`）を表す。`domain.model` に配置する |
| data class `TradeDecision` | 判定結果。`TradeAction` と判定理由、ATR を保持する（`domain.model`） |
| class `SimulationService` | Strategyの判定結果を受け取り、仮想資産の計算・更新を行う |

## 5. 配置予定のクラス・ファイル

| 種類 | 配置 | 役割 |
| --- | --- | --- |
| interface | `domain.strategy.TradingStrategy` | 戦略の共通インターフェース |
| data class | `domain.model.TradeDecision` | 売買の判定結果（判定の種類・理由・ATR） |
| enum | `domain.model.TradeAction` | 判定の種類 |
| class | `domain.strategy.CooldownReboundStrategy` | クールダウン機能を持つ戦略の実装 |
| class | `domain.simulation.SimulationService` | 仮想資産の更新ロジック |

## 6. 処理フロー

1. `application` が現在の設定から使用する Strategy 実装クラスを生成・取得する
2. `application` が直近のK線リストと現在の状態（`SimulationState`）を Strategy に渡す
3. Strategy が内部で計算（MAやATRなど）を行い、`TradeDecision` を返す
4. `application` がその `TradeDecision` を `SimulationService`（またはリアル注文サービス）に渡す
5. `SimulationService` が新しい状態（更新された `SimulationState`）を返す

## 7. Mermaid による設計フロー

```mermaid
flowchart TD
    A[Application] -->|1. K線, 状態| B(TradingStrategy)
    B -->|2. 計算・判定| B
    B -->|3. TradeDecision| A
    A -->|4. TradeDecision, 状態| C(SimulationService)
    C -->|5. 更新された状態| A
```

## 8. データの流れ

| データ | 発生元 | 渡し先 | 説明 |
| --- | --- | --- | --- |
| K線リスト | application | TradingStrategy | 判定に必要な過去データ |
| SimulationState | application | TradingStrategy | 現在の保有状況やクールダウン情報 |
| TradeDecision | TradingStrategy | application | BUY, SELL などの判定結果 |

## 9. 状態管理

Strategy 内で発生した「次に引き継ぐべき状態」は、`SimulationState` に記録して保存する。
例としては、損切りしてクールダウンを開始した時刻や ATR の値がある。Strategyクラス自体にプロパティとして保持してはいけない。

## 10. エラー処理設計

計算に必要なデータが不足している場合は、例外を投げない。安全側に倒して `SKIP` か `HOLDING` を返す。

## 11. 具体例

実装時に判断が分かれやすい箇所を、具体的な処理の流れで示します。

### 例1: クールダウン期間中の処理

- `application` が `CooldownReboundStrategy.judge()` を呼び出す。
- 渡された `SimulationState` に「最後に損切りした時刻」が記録されている。
- Strategy内で現在時刻と比較し、クールダウン期間中であると判定する。
- 買い条件（反発など）を計算する前に直ちに `TradeAction.SKIP` を返す。

## 12. テスト方針

| テスト対象 | 確認内容 |
| --- | --- |
| 各種Strategy | K線のパターンと状態（保有の有無など）を入力とし、期待する `TradeDecision` が返されるか網羅的にテストする |

## 13. 関連ドキュメント

- [CooldownReboundStrategy 仕様書](../specifications/strategies/cooldown-rebound-strategy.md)
