# 第3波バックログ（Issue 化して順次対応）

## 文書の目的

- [改善計画](README.md) の第1波・第2波には含めないが、放置してはいけない課題を管理する
- それぞれ独立した設計判断を伴うため、着手前に仕様を決める必要があるものを分けて置く

## 対象読者

開発メンバー、プロジェクト管理者

## 関連ドキュメント

- [改善計画](README.md)
- [指摘一覧と根拠](findings.md)

## 扱い

第1波・第2波を終えてから、下記を Issue として登録し優先度を付けてください。第1波・第2波の作業ファイルと違って**そのまま実装に着手できる粒度ではありません**。着手前に仕様を決める必要があります。

## 一覧

### 1. 市場データの妥当性検証を Strategy の前段に置く（実施済み）

- **対象**: [findings.md](findings.md) の Q(1)
- **問題**: [product.md](../overview/product.md) は「データが足りない、APIが失敗した、時刻がズレているなど、危ない状態では売買判断を見送ります」「システムの時刻と市場データの時刻にズレがある場合（例: ±60秒以上）、警告を出します。ズレが続く場合は売買を止めて見送りにします」と宣言しているが、実装されていない。[TradingApplication](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/application/TradingApplication.kt) は Ticker を取得してログに出すだけで、K線の鮮度や整合性の検証に使っていない。[Kline.kt](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/model/Kline.kt) は全フィールドが未検証の String で、[KlineCsvFileReader.kt](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/infrastructure/output/KlineCsvFileReader.kt) は `openTime` を文字列としてソートしている。
- **方向性**: `MarketDataValidator` を Strategy の前段に置き、API ステータス・銘柄・時刻の鮮度・間隔の連続性・重複・価格が正であること・`high >= open/close`・`low <= open/close`・Ticker との乖離を検証する。失敗時は状態を変えず `SKIP` にする。
- **重要度**: 高 / **工数感**: M
- **状態**: 実施済み（[PLAN02](../plans/plan02-order-safety-guards.md) の一環）。Ticker との乖離は注文価格の決定時に別途チェックしている。

### 2. API リトライを指数バックオフ＋HTTP ステータス検証に統一する（実施済み）

- **対象**: [findings.md](findings.md) の Q(2)、AC
- **問題**: [product.md](../overview/product.md) は「少しずつ間隔を空けながら最大3回まで再試行します（指数バックオフ）」と宣言しているが、[GmoPublicApiClient](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/infrastructure/exchange/gmo/GmoPublicApiClient.kt) は全例外を固定1秒間隔で再試行している。HTTP ステータスも GMO API の `status` も検証せずデコードしている。タイムアウト設定も無い。
- **方向性**: 接続・読み取りタイムアウト、HTTP / API ステータスの検証、429 の `Retry-After` 尊重、指数バックオフ＋jitter を共通化する。**自動リトライは冪等な GET に限定し、POST（発注）は注文照合を経てから判断する**（安易にリトライすると二重注文になる）。
- **重要度**: 高 / **工数感**: M
- **状態**: 実施済み（[PLAN02](../plans/plan02-order-safety-guards.md) の一環）。

### 3. 重複実行を抑止する（実施済み）

- **対象**: [findings.md](findings.md) の Q(3)
- **問題**: [product.md](../overview/product.md) は「重複して実行された場合は、二重に保存・更新しないようにスキップします」と宣言しているが、排他制御が無い。Cloud Scheduler が重複起動した場合、2つの Job が同じ `state.json` を読み書きする。
- **方向性**: GCS の世代条件付き書き込み（`if-generation-match`）か、実行中を示すロックファイルを使う。Phase1 の実害は小さいが、Phase3 では二重注文に直結する。
- **重要度**: 中（Phase3 では高） / **工数感**: M
- **状態**: ロックファイル方式で実施済み（[PLAN02](../plans/plan02-order-safety-guards.md) の一環）。**GCS の世代条件付き書き込みは未対応。** ファイルの排他作成が原子的であることに依存しており、GCS のマウント上では保証されない。実注文で完全な排他が必要になった時点で再検討する。

### 4. クールダウンの日跨ぎ fail-open を直す

- **対象**: [findings.md](findings.md) の R
- **問題**: [CooldownReboundStrategy](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/strategy/CooldownReboundStrategy.kt) と [TrendConfirmReboundStrategy](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/strategy/TrendConfirmReboundStrategy.kt) は、`lastStopLossTime` が渡された K線の中に見つからないとクールダウン期間外と判定する。通常実行では当日分の K線しか取得しないため、前日終盤に損切りした後で日付が変わると、クールダウンが予定より早く解除される。安全側ではない。
- **方向性**: `cooldownUntilOpenTime` または残り本数を `SimulationState` に保存し、epoch 時刻と interval から判定する。基準時刻が未来・不明・欠損なら安全側に `SKIP` とする。下記6の `Clock` 注入とセットで実施したい。
- **重要度**: 中 / **工数感**: S

### 5. RealTradingSafetyChecker の入力値検証を追加する（実施済み）

- **対象**: [findings.md](findings.md) の AB
- **問題**: [RealTradingSafetyChecker](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/realtrading/RealTradingSafetyChecker.kt) が `tradeAmount <= 0` / `currentPrice <= 0` / 上限値が0以下といった不正値を明示的に拒否していない。現状は下流（注文数量が0以下のチェックやゼロ除算例外）で偶然弾かれているだけ。安全境界は上流の妥当性を信用すべきではない。
- **方向性**: 安全チェックの冒頭で金額・価格・上限値の正数検証を行い、不正値は注文不可にする。
- **重要度**: 中（Phase3 着手前には必須） / **工数感**: S
- **状態**: 実施済み。[PLAN02](../plans/plan02-order-safety-guards.md) の一環として対応した。

### 6. ArchitectureTest の依存方向を厳格化し、Clock を注入する

- **対象**: [findings.md](findings.md) の AC
- **問題**: [ArchitectureTest](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/test/kotlin/cryptoautotrading/architecture/ArchitectureTest.kt) は `application.dependsOn(domain, infrastructure)` と `infrastructure.dependsOn(domain, application)` を許可しており、application ↔ infrastructure の相互依存が通ってしまう。また [RealTradingService](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/realtrading/RealTradingService.kt) などが domain 内から `LocalDateTime.now()` を直接呼んでいるため、日次上限のリセットや停止時刻のテストが実行時刻に依存する。
- **方向性**: 依存方向を application → domain、infrastructure → domain、presentation → 各層に限定する。`Clock` / `TimeProvider` を注入し、日付境界と再起動を固定時刻で検証できるようにする。
- **重要度**: 中 / **工数感**: M
- **注意**: [kotlin-layer-boundaries](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/.agents/skills/kotlin-layer-boundaries/SKILL.md) を読んでから着手すること。
- **状態**: `Clock` 注入は実施済み（[PLAN02](../plans/plan02-order-safety-guards.md) の一環）。**依存方向の厳格化は未実施。**

### 7. gcloud / Terraform を一本化する

- **対象**: [findings.md](findings.md) の C
- **問題**: Cloud Run Job と Cloud Scheduler の定義が GitHub Actions（`gcloud`）と Terraform の2箇所にある。`terraform apply` するワークフローは無く、[terraform-validate.yml](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/.github/workflows/terraform-validate.yml) は構文検証だけ。[development-policy.md](../infrastructure/gcp/development-policy.md) は「`gcloud` に依存しすぎない構成にする」と宣言しているが、実態は gcloud が正。
- **方向性**: どちらを正にするか決める。Terraform を正にする場合は `terraform apply` ワークフローと既存リソースの `terraform import` 手順の整備が必要（工数 L）。gcloud を正にする場合は Terraform コードの扱いを決める（削除するか、参照用として明記するか）。
- **重要度**: 中 / **工数感**: L
- **注意**: [pr10-config-fail-fast.md](pr10-config-fail-fast.md) で「環境変数の食い違いを消す」ところまでは実施済みの想定。

### 8. 6時境界で判定がスキップされる時間帯を減らす（実施済み）

- **対象**: [findings.md](findings.md) の X
- **問題**: [TradingApplication.resolveKlineTargetDate()](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/application/TradingApplication.kt) が朝6時を境に取得対象日を切り替えるため、毎日 6:00〜約7:15 は必要な本数（最大15本）が揃わず判定がスキップされる。保有中でも利確・損切りが働かない。
- **方向性**: 前日分と当日分の2日分を取得してマージする。[pr08-doc-consistency.md](pr08-doc-consistency.md) で挙動を仕様書に明記するところまでは実施済みの想定。
- **重要度**: 中 / **工数感**: M
- **状態**: 実施済み（[PLAN02](../plans/plan02-order-safety-guards.md) の一環）。
