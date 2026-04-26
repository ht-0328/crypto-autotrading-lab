# AGENTS.md

このリポジトリで作業するJulesやCodexを含む、すべてのAIコーディングエージェント向けの共通ルールです。

## 1. 実行ワークフローの強制（※最重要）
作業内容に応じて、**必ず最初に以下のドキュメントを読み込み（cat等）、そのフォーマットや指示をコンテキストにロードしてから**実行してください。推測での実行は禁止します。

* **Pull Requestを作成する場合**
  * 読込対象1: `.github/pull_request_template.md` (この見出し構成を一切省略・変更せずにそのまま使用すること)
  * 読込対象2: `docs/ai/review-checklist.md` (この基準を満たしているか確認すること)
* **仕様策定・タスク整理を行う場合**
  * 読込対象: `docs/ai/skills/spec-writer.SKILL.md`
* **コードの依存関係やレイヤー構造をレビューする場合**
  * 読込対象: `docs/ai/skills/kotlin-layer-guard.SKILL.md`
* **自動売買ロジックの安全性レビューを行う場合**
  * 読込対象: `docs/ai/skills/trading-safety-review.SKILL.md`

## 2. 基本方針
- 回答、説明、コミットメッセージ、PRタイトル、PR本文、README、docs、コメントは日本語で書く。
- APIキー、シークレット、トークン、個人情報、および `.env` ファイルを絶対にコミットしない。

## 3. プロジェクト構成
- 主なアプリケーションコード: `projects/crypto-autotrading-app`
- 設定ファイル: `config/application-gmo.yaml`、`config/application-wiremock.yaml`

## 4. Kotlin / Gradle 方針
- Kotlin、Gradle、Java、および主要ライブラリのバージョンを勝手に変更しない。
- 依存レイヤー（`domain`, `application`, `infrastructure`）の責務を混ぜない。
- 既存機能の動作を変えない整理を優先し、勝手に大規模なリファクタリングを行わない。

## 5. 実行・テスト
変更後は必ず以下のコマンドで実行およびテストの確認を行うこと。

* テスト実行: `cd projects/crypto-autotrading-app && ./gradlew test`
* ビルド確認: `cd projects/crypto-autotrading-app && ./gradlew build`
* アプリ起動: `cd projects/crypto-autotrading-app && ./gradlew run`
* モック指定起動: `cd projects/crypto-autotrading-app && APP_CONFIG_PATH=../../config/application-wiremock.yaml ./gradlew run`
