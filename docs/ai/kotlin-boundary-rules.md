# Kotlin本体の境界ルール

対象ディレクトリ:
`projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/`

## レイヤ責務
- `domain`: ビジネスルールとドメインモデル。
- `application`: ユースケースのオーケストレーションのみを担当。
- `infrastructure`: 外部I/O（API、設定、永続化、出力など）の実装を担当。

## 変更時の境界ルール
1. **`domain` 変更時は `infrastructure` の同時変更を禁止する。**
   - `infrastructure` 変更が必要な場合は、別PRとして分離する。
   - **例外（設定連携）**: `TradingConfig` のような設定モデルを追加・変更する場合、`ConfigLoader` や GitHub Actions の環境変数設定も同時変更が必要になることがあります。その場合は、ビジネスロジックの追加を `infrastructure` に入れないことを条件に、同一PRでの変更を許容します。
2. **`application` はオーケストレーションのみとし、ビジネスロジックを実装しない。**
   - 計算・判定などのドメイン知識は `domain` に配置する。
   - `application` は依存オブジェクトの接続、処理順序制御、入出力境界の調停に限定する。

## レビュー観点
- `application` 層にドメイン知識（閾値判定・売買判断など）が混入していないか。
- `domain` 変更PRに `infrastructure` 差分が混在していないか。
- 境界違反がある場合はPRを分割し、責務を再配置する。
