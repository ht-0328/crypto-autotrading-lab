# AI Agents Core Instructions

このリポジトリで作業するすべてのAIコーディングエージェント（Jules, Codex等）向けの最重要ルールです。

## 1. ワークフローの強制ルール（必ず最初に行うこと）
作業を開始する前に、自身のタスク内容に応じて **必ず以下のドキュメントを読み込み（cat等）、制約とフォーマットをコンテキストにロード** してください。

* **基本の制約と期待動作**
  * 読込対象: [`docs/ai/agents-guidelines.md`](docs/ai/agents-guidelines.md) （絶対禁止事項とチェック項目）
  * 読込対象: [`docs/ai/prompt-template.md`](docs/ai/prompt-template.md) （出力フォーマット）

* **Pull Requestを作成・レビューする場合**
  * 読込対象: [`.github/pull_request_template.md`](.github/pull_request_template.md)
  * 読込対象: [`docs/ai/review-checklist.md`](docs/ai/review-checklist.md)

* **コード変更の粒度やレイヤー境界を扱う場合**
  * 読込対象: [`docs/ai/change-granularity.md`](docs/ai/change-granularity.md)
  * 読込対象: [`docs/ai/kotlin-boundary-rules.md`](docs/ai/kotlin-boundary-rules.md)

* **Kotlinコードを追加・変更する場合**
  * 読込対象: [`docs/ai/kotlin-boundary-rules.md`](docs/ai/kotlin-boundary-rules.md)
  * 読込対象: [`docs/ai/skills/kotlin-readable-code.SKILL.md`](docs/ai/skills/kotlin-readable-code.SKILL.md)
  * 方針: `let` / `run` / `with` / `apply` / `also` / 拡張関数は、コードの意図が明確になり、直感的に読める場合は積極的に使うこと。ただし、売買ロジック・設定・テストの意図が分かりにくくなる場合は、通常の関数、明示的な変数名、`if`、`return` を優先すること。

* **AIスキルの適用が必要な場合**
  * 読込対象: [`docs/ai/skills-catalog.md`](docs/ai/skills-catalog.md)
  * さらに、必要なスキルの詳細定義（例: [`docs/ai/skills/spec-writer.SKILL.md`](docs/ai/skills/spec-writer.SKILL.md)）を読み込むこと。

## 2. 絶対厳守事項
- **言語**: 回答、説明、コミットメッセージ、PRタイトル・本文、コードコメントはすべて**日本語**で記述すること（技術用語は英語のまま）。
- **機密保持**: APIキー、シークレット、トークン、個人情報、`.env` ファイルは絶対に生成・出力・コミットしないこと。
- **検証責任**: コード変更後は必ずローカルでテスト（`./gradlew test`）およびビルド（`./gradlew build`）を実行し、成功を確認してから報告すること。
