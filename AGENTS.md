# AGENTS.md

crypto-autotrading-lab で作業する AI コーディングエージェント共通のルールです。

Codex と Antigravity はこのファイルを自動で読み込みます。Claude Code は `AGENTS.md` を読まないため、[CLAUDE.md](CLAUDE.md) が `@AGENTS.md` でこのファイルを取り込みます。仕組みの詳細は [.agents/README.md](.agents/README.md) を参照してください。

## このリポジトリについて

- 仮想通貨の自動売買アプリです。Kotlin / JVM 17 / Gradle で作られており、本体は [projects/crypto-autotrading-app/](projects/crypto-autotrading-app/) にあります。
- 現在は Phase1 です。実資金での実注文は行いません。判断に幅があるときは常に安全側（損失を出さない側）に倒してください。
- 全体像は [README.md](README.md)、ドキュメントの地図は [docs/README.md](docs/README.md) を参照してください。

## 絶対厳守

- **日本語で書く**: 回答、説明、コミットメッセージ、PR本文、コードコメント、KDoc はすべて日本語で書きます（技術用語は英語のままで構いません）。
- **秘密情報を出さない**: APIキー、シークレット、トークン、個人情報、`.env` の中身を生成・出力・コミットしません。ログにも出しません。
- **危険な操作をしない**: 本番環境への直接デプロイ、破壊的DB操作（`DROP` / `TRUNCATE` 等）、実資金での発注、事前承認のない依存ライブラリのメジャーバージョンアップは行いません。
- **スコープを勝手に広げない**: 依頼されていないファイルを変更しません。判断に迷ったら推測で進めず、ユーザーに確認します。
- **依頼の条件を守る**: 変更対象・変更禁止領域・受け入れ条件が指定されている場合は、それを厳守します。既存の公開API、設定キー、その意味を壊しません。

## 変更したら必ず検証する

Kotlin コード、Gradle 設定、`config/` の設定ファイルなど、ビルドや動作に影響する変更をした場合は、以下を実行して結果を報告してください。

```bash
cd projects/crypto-autotrading-app
./gradlew build
```

`build` にはコンパイル、全単体テスト、アーキテクチャテスト（Konsist）が含まれます。

- 実行していない検証を「実行した」と書いてはいけません。失敗したときは、失敗した事実とログを報告してください。
- Lint / フォーマッタ（detekt, ktlint）はこのリポジトリに導入されていません。存在しないコマンドを実行したことにしないでください。

## 作業内容ごとの詳細ルール

詳細ルールは [.agents/skills/](.agents/skills/) にスキルとして置いてあります。該当する作業をするときは、そのスキルの `SKILL.md` を読んでから着手してください。

| 作業内容 | 読むスキル |
| --- | --- |
| Kotlin コードを書く・直す | [kotlin-code-rules](.agents/skills/kotlin-code-rules/SKILL.md) |
| レイヤ構成や依存関係に触る | [kotlin-layer-boundaries](.agents/skills/kotlin-layer-boundaries/SKILL.md) |
| コミットメッセージ・PRタイトル・PR本文を書く | [pr-and-commit](.agents/skills/pr-and-commit/SKILL.md) |
| コード変更後に自己点検する / PRをレビューする | [code-review](.agents/skills/code-review/SKILL.md) |
| 曖昧な要望を仕様に落とす | [spec-writer](.agents/skills/spec-writer/SKILL.md) |
| 売買ロジックの安全性を点検する | [trading-safety-review](.agents/skills/trading-safety-review/SKILL.md) |

Codex / Antigravity / Claude Code はこれらを自動で検出します。スキル機構を持たないツールを使う場合は、上表のファイルを直接読んでください。

## 完了報告

ファイルを変更する作業が終わったら、以下の3点をチャットで報告してください（質問への回答やレビューだけの場合は不要です）。

1. **変更点**: どのファイルを、なぜ、どう変えたか。
2. **検証結果**: 実行したコマンドと成否。実行していないものは「未実行」と書く。
3. **リスク・未対応**: 残っている懸念、ユーザーに判断してほしい点。

PR本文はこの完了報告とは別物です。PR本文は [.github/pull_request_template.md](.github/pull_request_template.md) の形式に従い、チャット報告用の項目を混ぜないでください。
