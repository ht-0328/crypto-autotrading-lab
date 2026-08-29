---
name: kotlin-layer-boundaries
description: >-
  crypto-autotrading-lab のレイヤ境界（domain / application / infrastructure / presentation）を
  設計・変更・監査するためのルールと、Konsist の ArchitectureTest の扱い方。
  package の移動、import や依存関係の変更、interface / Repository / DTO の追加、
  外部I/Oの配置、DI配線、ArchitectureTest の変更をするとき、
  および依存違反を監査するときに使用する。
---

# レイヤ境界ルール

対象ディレクトリ: `projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/`

## 1. 各レイヤの責務

| レイヤ | 責務 | 依存してよい先 |
| --- | --- | --- |
| `domain` | ビジネスルールとドメインモデル。売買判定、損益計算など。 | なし（他のどのレイヤにも依存しない） |
| `application` | ユースケースのオーケストレーションのみ。処理順序の制御と入出力境界の調停。 | `domain` |
| `infrastructure` | 外部I/O（GMO API、設定読み込み、CSV/JSON の永続化、ログ出力）の実装。 | `domain` |
| `presentation` | エントリポイントと DI 配線。 | `domain`, `application`, `infrastructure` |

上の表の「依存してよい先」だけが許可された依存です。表にない向きの依存は作らないでください。

## 2. 守るべき境界

1. **`domain` に外部I/Oを持ち込まない**
   - HTTPクライアント、ファイル操作、DB、環境変数アクセス、現在時刻の直接取得を `domain` に書かないでください。
   - 必要な場合は `domain` に interface を置き、実装を `infrastructure` に置いてください。
2. **`infrastructure` の型を `domain` に漏らさない**
   - GMO API のレスポンス型（`infrastructure.exchange.gmo.model` 等）を、`domain` の interface の引数や戻り値に出さないでください。
   - `infrastructure` 側で `domain` のモデルへ変換してから渡してください。
3. **`application` にビジネスロジックを書かない**
   - 閾値判定、売買判断、損益計算などのドメイン知識は `domain` に置いてください。
   - `application` は依存オブジェクトの接続、処理順序の制御、入出力境界の調停に限定します。
4. **`domain` 変更と `infrastructure` 変更を同じPRに混ぜない**
   - `infrastructure` の変更が必要な場合は、原則として別PRに分けてください。
   - **例外（設定連携）**: `TradingConfig` のような設定モデルを追加・変更する場合、`ConfigLoader` や GitHub Actions の環境変数設定も同時に変更しないと動作確認できないことがあります。その場合は、ビジネスロジックを `infrastructure` に入れないことを条件に、同一PRを許容します。理由はPR本文に書いてください。

## 3. Konsist によるアーキテクチャテスト

境界ルールは目視ではなく、テストで検査します。

- テストの場所: `projects/crypto-autotrading-app/src/test/kotlin/cryptoautotrading/architecture/ArchitectureTest.kt`
- 実行方法: `cd projects/crypto-autotrading-app && ./gradlew build`（`build` に含まれます）
- CI では [.github/workflows/ci.yml](../../../.github/workflows/ci.yml) の `test` ジョブで実行されます。

現在このテストが検査している内容は次のとおりです。

- 各レイヤ間の依存方向
- `domain` の interface に `infrastructure` の型が漏れていないこと
- 1ファイルに `data class` が複数定義されていないこと
- プロダクションコードの class / interface / 関数（`private` 含む）に KDoc があること

新しい境界ルールを増やしたときは、このテストにも検査を追加してください。既存ルールの範囲内の変更では、テストを増やす必要はありません。

**注意**: 現在のテストの依存方向の定義は、上の表より緩く、`application` ⇄ `infrastructure` の相互依存を許してしまっています。実コードに違反はありませんが、判断の正は上の表です。テスト側を表に合わせて締める作業は未対応です。

## 4. 違反を見つけたときの報告

指摘だけで終わらせず、以下をセットで出してください。

- 違反箇所（ファイルパスと行番号）と、どのルールに違反しているか。
- 重大度（高: `domain` の独立性を壊す / 中: 責務の配置ミス / 低: 命名や配置の一貫性）。重大度の高い順に並べて出してください。
- 具体的な修正案（コード差分の形で）。
- 同じ違反を再発させないための検査案（`ArchitectureTest` への追加テストなど）。
