# AI Agents Core Instructions

このファイルは、このリポジトリで作業する AI コーディングエージェント向けの入口です。

## 作業別に読む資料

### 作業を開始するとき

まず以下を読んでください。

- [AGENTS.md](./AGENTS.md)
- [docs/ai/agents-guidelines.md](./docs/ai/agents-guidelines.md)
- [docs/ai/prompt-template.md](./docs/ai/prompt-template.md)

### コミットメッセージを作成するとき

まず以下を読んでください。

- [docs/ai/pr-and-commit-rules.md](./docs/ai/pr-and-commit-rules.md)

### PRタイトルを作成するとき

まず以下を読んでください。

- [docs/ai/pr-and-commit-rules.md](./docs/ai/pr-and-commit-rules.md)

### PR本文を作成・更新するとき

まず以下を読んでください。

- [.github/pull_request_template.md](./.github/pull_request_template.md)
- [docs/ai/pr-and-commit-rules.md](./docs/ai/pr-and-commit-rules.md)

### PRをレビューするとき

まず以下を読んでください。

- [.github/pull_request_template.md](./.github/pull_request_template.md)
- [docs/ai/review-checklist.md](./docs/ai/review-checklist.md)
- [docs/ai/pr-and-commit-rules.md](./docs/ai/pr-and-commit-rules.md)

### Kotlinコードを変更するとき

まず以下を読んでください。

- [docs/ai/kotlin-boundary-rules.md](./docs/ai/kotlin-boundary-rules.md)
- [docs/ai/skills/kotlin-readable-code.SKILL.md](./docs/ai/skills/kotlin-readable-code.SKILL.md)

### 変更粒度や責務分担を判断するとき

まず以下を読んでください。

- [docs/ai/change-granularity.md](./docs/ai/change-granularity.md)
- [docs/ai/kotlin-boundary-rules.md](./docs/ai/kotlin-boundary-rules.md)

### 仕様を書くとき

まず以下を読んでください。

- [docs/ai/skills/spec-writer.SKILL.md](./docs/ai/skills/spec-writer.SKILL.md)

### AIスキルを使う必要があるとき

まず以下を読んでください。

- [docs/ai/skills-catalog.md](./docs/ai/skills-catalog.md)

必要なスキル定義ファイルがある場合は、そのファイルも読んでください。

## ドキュメント読み込みに関する禁止事項

- `cat` を実行せずに、過去の記憶や推測だけで作業してはいけない
- `AGENTS.md` だけを読んで、関連ドキュメントを省略してはいけない
- ユーザーの依頼が短い場合でも、読み込み手順を省略してはいけない
- PR本文を作る前に `.github/pull_request_template.md` を読まず、独自形式でPR本文を作ってはいけない
- `cat` が使えない環境では、`sed -n '1,240p' <file>` など本文を確認できる同等コマンドを使い、その理由を完了報告に書くこと

## 絶対厳守事項

- **言語**: 回答、説明、コードコメントはすべて**日本語**で記述すること（技術用語は英語のまま）。
- **機密保持**: APIキー、シークレット、トークン、個人情報、`.env` ファイルは絶対に生成・出力・コミットしないこと。
- **検証責任**: コード変更後は必ずローカルでテスト（`./gradlew test`）およびビルド（`./gradlew build`）を実行し、成功を確認してから報告すること。

## 完了報告のルール

作業完了時、またはPR本文を作成・更新する場合は、以下のように「読んだドキュメント」を明記すること。該当しないものがある場合は、削除せずに `該当なし` と理由を書くこと。

```markdown
## AI確認済みドキュメント

- [x] `AGENTS.md`
- [x] `docs/ai/agents-guidelines.md`
- [x] `docs/ai/prompt-template.md`
- [x] `docs/ai/pr-and-commit-rules.md`
- [x] `.github/pull_request_template.md`（PR作成・更新・レビュー時）
- [x] `docs/ai/review-checklist.md`（PR作成・更新・レビュー時）
```
