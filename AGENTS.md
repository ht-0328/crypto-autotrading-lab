# AI Agents Core Instructions

このリポジトリで作業するすべてのAIコーディングエージェント（Jules, Codex等）向けの最重要ルールです。

## 1. ワークフローの強制ルール（必ず最初に行うこと）

作業開始直後に、必ず以下を実行すること。

- **基本の制約と期待動作**
  - 読込対象: [`AGENTS.md`](AGENTS.md)
  - 読込対象: [`docs/ai/agents-guidelines.md`](docs/ai/agents-guidelines.md) （絶対禁止事項とチェック項目）
  - 読込対象: [`docs/ai/prompt-template.md`](docs/ai/prompt-template.md) （出力フォーマット）
  - 読込対象: [`docs/ai/pr-and-commit-rules.md`](docs/ai/pr-and-commit-rules.md) （コミット・PR作成の詳細ルール）

```bash
cat AGENTS.md
cat docs/ai/agents-guidelines.md
cat docs/ai/prompt-template.md
cat docs/ai/pr-and-commit-rules.md
```

Pull Requestを作成・更新・レビューする場合は、追加で以下を実行すること。

- **Pull Requestを作成・レビューする場合**
  - 読込対象: [`.github/pull_request_template.md`](.github/pull_request_template.md)
  - 読込対象: [`docs/ai/review-checklist.md`](docs/ai/review-checklist.md)

```bash
cat .github/pull_request_template.md
cat docs/ai/review-checklist.md
```

Kotlinコードを追加・変更する場合は、追加で以下を実行すること。

- **Kotlinコードを追加・変更する場合**
  - 読込対象: [`docs/ai/kotlin-boundary-rules.md`](docs/ai/kotlin-boundary-rules.md)
  - 読込対象: [`docs/ai/skills/kotlin-readable-code.SKILL.md`](docs/ai/skills/kotlin-readable-code.SKILL.md)

```bash
cat docs/ai/kotlin-boundary-rules.md
cat docs/ai/skills/kotlin-readable-code.SKILL.md
```

方針: `let` / `run` / `with` / `apply` / `also` / 拡張関数は、コードの意図が明確になり、直感的に読める場合は積極的に使うこと。ただし、売買ロジック・設定・テストの意図が分かりにくくなる場合は、通常の関数、明示的な変数名、`if`、`return` を優先すること。

コード変更の粒度やレイヤー境界を扱う場合は、追加で以下を実行すること。

- **コード変更の粒度やレイヤー境界を扱う場合**
  - 読込対象: [`docs/ai/change-granularity.md`](docs/ai/change-granularity.md)
  - 読込対象: [`docs/ai/kotlin-boundary-rules.md`](docs/ai/kotlin-boundary-rules.md)

```bash
cat docs/ai/change-granularity.md
cat docs/ai/kotlin-boundary-rules.md
```

AIスキルを使う必要がある場合は、追加で以下を実行すること。

- **AIスキルの適用が必要な場合**
  - 読込対象: [`docs/ai/skills-catalog.md`](docs/ai/skills-catalog.md)

```bash
cat docs/ai/skills-catalog.md
```

さらに、必要なスキル定義ファイルがある場合は、そのファイルも `cat` で読むこと。

例:

- 読込対象: [`docs/ai/skills/spec-writer.SKILL.md`](docs/ai/skills/spec-writer.SKILL.md)

```bash
cat docs/ai/skills/spec-writer.SKILL.md
```

### ドキュメント読み込みに関する禁止事項

- `cat` を実行せずに、過去の記憶や推測だけで作業してはいけない
- `AGENTS.md` だけを読んで、関連ドキュメントを省略してはいけない
- ユーザーの依頼が短い場合でも、読み込み手順を省略してはいけない
- PR本文を作る前に `.github/pull_request_template.md` を読まず、独自形式でPR本文を作ってはいけない
- `cat` が使えない環境では、`sed -n '1,240p' <file>` など本文を確認できる同等コマンドを使い、その理由を完了報告に書くこと

## 2. 絶対厳守事項

- **言語**: 回答、説明、コミットメッセージ、PRタイトル・本文、コードコメントはすべて**日本語**で記述すること（技術用語は英語のまま）。
- **機密保持**: APIキー、シークレット、トークン、個人情報、`.env` ファイルは絶対に生成・出力・コミットしないこと。
- **検証責任**: コード変更後は必ずローカルでテスト（`./gradlew test`）およびビルド（`./gradlew build`）を実行し、成功を確認してから報告すること。

## 3. コミット・PR作成ルールの絶対遵守事項

- コミットメッセージ、PRタイトル、PR本文の詳細ルールは `docs/ai/pr-and-commit-rules.md` を必ず確認すること。
- PR本文を作成する場合は、必ず `.github/pull_request_template.md` を読み直すこと。
- PR本文はテンプレートの見出し、順番、チェックボックスを維持すること。
- PRテンプレートにない独自見出しを追加しないこと。
- 作業完了報告用の項目をPR本文に混ぜないこと。

## 4. 完了報告のルール

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
