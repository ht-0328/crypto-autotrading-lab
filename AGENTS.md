# AI Agents Core Instructions

このリポジトリで作業するすべてのAIコーディングエージェント（Jules, Codex等）向けの最重要ルールです。

## 1. ワークフローの強制ルール（必ず最初に行うこと）

作業開始直後に、必ず以下を実行すること。

```bash
cat AGENTS.md
cat docs/ai/agents-guidelines.md
cat docs/ai/prompt-template.md
```

Pull Requestを作成・更新・レビューする場合は、追加で以下を実行すること。

```bash
cat .github/pull_request_template.md
cat docs/ai/review-checklist.md
```

Kotlinコードを追加・変更する場合は、追加で以下を実行すること。

```bash
cat docs/ai/kotlin-boundary-rules.md
cat docs/ai/skills/kotlin-readable-code.SKILL.md
```

方針: `let` / `run` / `with` / `apply` / `also` / 拡張関数は、コードの意図が明確になり、直感的に読める場合は積極的に使うこと。ただし、売買ロジック・設定・テストの意図が分かりにくくなる場合は、通常の関数、明示的な変数名、`if`、`return` を優先すること。

コード変更の粒度やレイヤー境界を扱う場合は、追加で以下を実行すること。

```bash
cat docs/ai/change-granularity.md
cat docs/ai/kotlin-boundary-rules.md
```

AIスキルを使う必要がある場合は、追加で以下を実行すること。

```bash
cat docs/ai/skills-catalog.md
```

必要なスキル定義ファイルがある場合は、そのファイルも `cat` で読むこと。

例:

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

### コミットメッセージのルール

- コミットメッセージは日本語で書くこと。
- `feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:` などの接頭辞は使ってよい。
- ただし、接頭辞の後ろの説明は必ず日本語にすること。
- 英語だけのコミットメッセージは禁止すること。
- `Update files`、`Fix bug`、`Changes` のような曖昧なコミットメッセージは禁止すること。

**良い例**:

- `docs: AGENTS.mdにコミットルールを追加`
- `fix: PRテンプレートに沿うように説明を修正`
- `feat: 売買戦略の切り替え設定を追加`
- `refactor: 売買判定ロジックの責務を整理`

**悪い例**:

- `Update files`
- `Fix bug`
- `Changes`
- `Improve docs`
- `modify ci`

### PRタイトルのルール

- PRタイトルは日本語で書くこと。
- PRタイトルにも `feat:`, `fix:`, `docs:` などの接頭辞は使ってよい。
- ただし、接頭辞の後ろの説明は必ず日本語にすること。
- 英語だけのPRタイトルは禁止すること。
- 内容が分からないPRタイトルは禁止すること。

**良い例**:

- `docs: AGENTS.mdにPR作成ルールを追加`
- `fix: PR本文がテンプレートに沿うように修正`
- `feat: 売買戦略の切り替え設定を追加`

**悪い例**:

- `Update AGENTS`
- `Fix`
- `Add changes`
- `Improve docs`

### PR本文の作成ルール

- PR本文は必ず `.github/pull_request_template.md` を使うこと。
- PRテンプレートの見出しを削除しないこと。
- PRテンプレートの項目を勝手に並べ替えないこと。
- 空欄のままPRを作成しないこと。
- 該当しない項目は `該当なし` と書くこと。
- チェックボックスを勝手に削除しないこと。
- 実行していない確認やテストを、実行済みのように書かないこと。
- テストや確認を実行できなかった場合は、その理由を日本語で書くこと。

### 違反時の修正ルール

- コミットメッセージが英語だけの場合は、日本語に修正すること。
- PRタイトルが英語だけの場合は、日本語に修正すること。
- PR本文がテンプレート通りでない場合は、テンプレートに沿って書き直すこと。
- PR本文に空欄がある場合は、具体的な内容または `該当なし` を記入すること。
- ユーザーから「テンプレ通りではない」「日本語になっていない」と指摘された場合は、該当箇所を修正すること。

## 4. 完了報告のルール

作業完了時、またはPR本文を作成・更新する場合は、以下のように「読んだドキュメント」を明記すること。該当しないものがある場合は、削除せずに `該当なし` と理由を書くこと。

```markdown
## AI確認済みドキュメント

- [x] `AGENTS.md`
- [x] `docs/ai/agents-guidelines.md`
- [x] `docs/ai/prompt-template.md`
- [x] `.github/pull_request_template.md`（PR作成・更新・レビュー時）
- [x] `docs/ai/review-checklist.md`（PR作成・更新・レビュー時）
```
