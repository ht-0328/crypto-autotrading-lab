# 第3波バックログ（Issue 化して順次対応）

| 項目 | 内容 |
| --- | --- |
| 想定読者 | 第3波の課題を Issue 化する人 |
| 読んだあとできること | 残っている課題の内容と根拠を把握し、着手順を決められる |
| 状態 | 現行 |
| 機密区分 | 公開可 |
| 作成者 | リポジトリ管理者 |
| 保守責任者 | リポジトリ管理者 |
| 最終確認日 | 2026-08-30 |


## 文書の目的

- [改善計画](README.md) の第1波・第2波には含めないが、放置してはいけない課題を管理する
- それぞれ独立した設計判断を伴うため、着手前に仕様を決める必要があるものを分けて置く

## 対象読者

開発メンバー、プロジェクト管理者

## 関連ドキュメント

- [改善計画](README.md)
- [指摘一覧と根拠](findings.md)

## 扱い

第1波・第2波を終えてから、下記を Issue として登録し優先度を付ける。第1波・第2波の作業ファイルと違って**そのまま実装に着手できる粒度ではありません**。着手前に仕様を決める必要がある。

## 一覧

着手できる単位に分けた項目である。番号は優先順位ではない。

### 1. 市場データの妥当性検証を Strategy の前段に置く（実施済み）

- **対象**: [findings.md](findings.md) の Q(1)
- **問題**: [product.md](../overview/product.md) は次の2つを宣言しているが、実装されていない。
  - 「データが足りない、APIが失敗した、時刻がズレているなど、危ない状態では売買判断を見送ります」
  - 「システムの時刻と市場データの時刻にズレがある場合（例: ±60秒以上）、警告を出します。ズレが続く場合は売買を止めて見送りにします」

  [TradingApplication](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/application/TradingApplication.kt) は Ticker を取得してログに出すだけである。K線の鮮度や整合性の検証には使っていない。[Kline.kt](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/model/Kline.kt) は全フィールドが未検証の String である。
  [KlineCsvFileReader.kt](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/infrastructure/output/KlineCsvFileReader.kt) は `openTime` を文字列としてソートしている。
- **方向性**: `MarketDataValidator` を Strategy の前段に置く。検証項目は次のとおり。

  | 対象 | 見るもの |
  | --- | --- |
  | API | ステータス、銘柄 |
  | 時刻 | 鮮度、間隔の連続性、重複 |
  | 価格 | 正の値であること、高安の整合、Ticker との乖離 |
失敗時は状態を変えず `SKIP` にする。
- **重要度**: 高 / **工数感**: M
- **状態**: 実施済み（[PLAN02](../plans/plan02-order-safety-guards.md) の一環）。Ticker との乖離は注文価格の決定時に別途チェックしている。

### 2. API リトライを指数バックオフ＋HTTP ステータス検証に統一する（実施済み）

- **対象**: [findings.md](findings.md) の Q(2)、AC
- **問題**: [product.md](../overview/product.md) の宣言と実装が違う。宣言は「最大3回まで再試行します（指数バックオフ）」。しかし [GmoPublicApiClient](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/infrastructure/exchange/gmo/GmoPublicApiClient.kt) は全例外を固定1秒間隔で再試行している。HTTP ステータスも GMO API の `status` も検証せずデコードしている。タイムアウト設定も無い。
- **方向性**: 次を共通化する。タイムアウト、ステータス検証、429 の `Retry-After` 尊重、指数バックオフと jitter。**自動リトライは冪等な GET に限定する。POST（発注）は注文照合を経てから判断する。** 安易にリトライすると二重注文になる。
- **重要度**: 高 / **工数感**: M
- **状態**: 実施済み（[PLAN02](../plans/plan02-order-safety-guards.md) の一環）。

### 3. 重複実行を抑止する（実施済み）

- **対象**: [findings.md](findings.md) の Q(3)
- **問題**: [product.md](../overview/product.md) は「重複実行では二重に保存・更新しない」と宣言している。しかし排他制御が無い。Cloud Scheduler が重複起動した場合、2つの Job が同じ `state.json` を読み書きする。
- **方向性**: GCS の世代条件付き書き込み（`if-generation-match`）か、実行中を示すロックファイルを使う。Phase1 の実害は小さいが、Phase3 では二重注文に直結する。
- **重要度**: 中（Phase3 では高） / **工数感**: M
- **状態**: ロックファイル方式で実施済み（[PLAN02](../plans/plan02-order-safety-guards.md) の一環）。**GCS の世代条件付き書き込みは未対応。** ファイルの排他作成が原子的であることに依存しており、GCS のマウント上では保証されない。実注文で完全な排他が必要になった時点で再検討する。

### 4. クールダウンの日跨ぎ fail-open を直す

- **対象**: [findings.md](findings.md) の R
- **問題**: [Cooldown](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/strategy/CooldownReboundStrategy.kt) と [TrendConfirm](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/strategy/TrendConfirmReboundStrategy.kt) に穴がある。
  `lastStopLossTime` が渡された K線の中に無いと、クールダウン期間外と判定する。通常実行では当日分の K線しか取得しない。前日終盤に損切りした後で日付が変わると、クールダウンが予定より早く解除される。安全側ではない。
- **方向性**: `cooldownUntilOpenTime` か残り本数を `SimulationState` に保存する。判定は epoch 時刻と interval で行う。基準時刻が未来・不明・欠損なら安全側に `SKIP` とする。下記6の `Clock` 注入とセットで実施したい。
- **重要度**: 中 / **工数感**: S

### 5. RealTradingSafetyChecker の入力値検証を追加する（実施済み）

- **対象**: [findings.md](findings.md) の AB
- **問題**: [RealTradingSafetyChecker](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/realtrading/RealTradingSafetyChecker.kt) が不正値を明示的に拒否していない。対象は `tradeAmount <= 0`、`currentPrice <= 0`、上限値が0以下の3つ。現状は下流（注文数量が0以下のチェックやゼロ除算例外）で偶然弾かれているだけ。安全境界は上流の妥当性を信用すべきではない。
- **方向性**: 安全チェックの冒頭で金額・価格・上限値の正数検証を行い、不正値は注文不可にする。
- **重要度**: 中（Phase3 着手前には必須） / **工数感**: S
- **状態**: 実施済み。[PLAN02](../plans/plan02-order-safety-guards.md) の一環として対応した。

### 6. ArchitectureTest の依存方向を厳格化し、Clock を注入する

- **対象**: [findings.md](findings.md) の AC
- **問題**: [ArchitectureTest](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/test/kotlin/cryptoautotrading/architecture/ArchitectureTest.kt) が相互依存を許している。`application.dependsOn(...)` と `infrastructure.dependsOn(...)` の両方を許可しているためである。また [RealTradingService](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/realtrading/RealTradingService.kt) などが domain 内から `LocalDateTime.now()` を直接呼んでいる。日次上限のリセットや停止時刻のテストが実行時刻に依存する。
- **方向性**: 依存方向を限定する。許すのは次の3方向のみとする。

  - application → domain
  - infrastructure → domain
  - presentation → 各層

  あわせて `Clock` を注入し、日付境界と再起動を固定時刻で検証できるようにする。
- **重要度**: 中 / **工数感**: M
- **注意**: [kotlin-layer-boundaries](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/.agents/skills/kotlin-layer-boundaries/SKILL.md) を読んでから着手すること。
- **状態**: `Clock` 注入は実施済み（[PLAN02](../plans/plan02-order-safety-guards.md) の一環）。**依存方向の厳格化は未実施。**

### 7. gcloud / Terraform を一本化する

- **対象**: [findings.md](findings.md) の C
- **問題**: 定義が2箇所にある。GitHub Actions（`gcloud`）と Terraform である。対象は Cloud Run Job と Cloud Scheduler。`terraform apply` するワークフローは無く、[terraform-validate.yml](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/.github/workflows/terraform-validate.yml) は構文検証だけ。[development-policy.md](../infrastructure/gcp/development-policy.md) は「`gcloud` に依存しすぎない構成にする」と宣言している。実態は gcloud が正である。
- **方向性**: どちらを正にするか決める。Terraform を正にするなら、`terraform apply` ワークフローと `terraform import` 手順が要る（工数 L）。gcloud を正にするなら、Terraform コードを削除するか参照用と明記するかを決める。
- **重要度**: 中 / **工数感**: L
- **注意**: [PR10](pr10-config-fail-fast.md) で「環境変数の食い違いを消す」ところまでは実施済みの想定。

### 8. 6時境界で判定がスキップされる時間帯を減らす（実施済み）

- **対象**: [findings.md](findings.md) の X
- **問題**: [resolveKlineTargetDate()](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/application/TradingApplication.kt) が取得対象日を朝6時で切り替える。
  そのため毎日 6:00〜約7:15 は必要な本数（最大15本）が揃わず、判定がスキップされる。保有中でも利確・損切りが働かない。
- **方向性**: 前日分と当日分の2日分を取得してマージする。[pr08-doc-consistency.md](pr08-doc-consistency.md) で挙動を仕様書に明記するところまでは実施済みの想定。
- **重要度**: 中 / **工数感**: M
- **状態**: 実施済み（[PLAN02](../plans/plan02-order-safety-guards.md) の一環）。
