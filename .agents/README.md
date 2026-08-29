# .agents/

AIコーディングエージェント向けの設定を置くディレクトリです。

## なぜこの構成か

Claude Code / Codex / Antigravity は、それぞれ自動で読み込むファイルが違います。次の表は、各ツールに実際にスキルを列挙させて確認した結果です。

検証日: 2026-08-29 / Claude Code 2.1.236, codex-cli 0.151.0-alpha.7.1, agy 1.1.22
（各ツールの仕様は変わることがあります。構成を触るときは下の「検証方法」で再確認してください。）

| | 自動で読み込む指示ファイル | 自動で検出するスキル |
| --- | --- | --- |
| Claude Code | `CLAUDE.md`（`AGENTS.md` は読まない） | `.claude/skills/<name>/SKILL.md` |
| Codex | `AGENTS.md`（上位ディレクトリへ遡ってマージ） | `.codex/skills/` と `.agents/skills/` |
| Antigravity | `AGENTS.md` / `GEMINI.md` / `.agents/rules/*.md` | `.agents/skills/<name>/SKILL.md` |

上の表は各ツールの仕様です。このリポジトリでは `GEMINI.md` と `.agents/rules/` は使っていません（ルールは `AGENTS.md` に集約）。

3ツールの共通項がないため、**正典を1つ置き、残りはブリッジでつなぐ**方針にしています。

- ルールの正典は [../AGENTS.md](../AGENTS.md)。Codex と Antigravity はこれを直接読みます。
- [../CLAUDE.md](../CLAUDE.md) は `@AGENTS.md` で `AGENTS.md` を取り込むだけです。ルール本文をここに複製しないでください。
- スキルの正典は [skills/](skills/)。Codex と Antigravity はこれを直接検出します。
- `.claude/skills` は `../.agents/skills` へのシンボリックリンクです。Claude Code はこれ経由で同じスキルを検出します。

同じ内容を複数の場所にコピーしないでください。片方だけ更新されて食い違う原因になります。

補足:

- サブディレクトリに `AGENTS.md` を置くと、Codex と Antigravity はそのディレクトリ配下で追加ルールとしてマージします（深い階層が優先）。現在このリポジトリでは使っていません。
- `.claude/skills` はシンボリックリンクです。Windows でチェックアウトする場合は `git config core.symlinks true` が必要です（開発は devcontainer 上の Linux を前提としています）。

## スキルの書き方

`skills/<スキル名>/SKILL.md` に置き、先頭にYAMLフロントマターを付けます。

```markdown
---
name: skill-name
description: >-
  このスキルが何をするか、どういうときに使うかを書く。
  エージェントはこの description だけを見て、読み込むかどうかを決める。
---

# 見出し

手順や判断基準を書く。
```

- `name` は小文字とハイフンのみ。ディレクトリ名と揃えてください。
- `description` が最も重要です。「何をするか」と「いつ使うか」の両方を書いてください。
- 本文が長くなる場合は `references/` に分けてリンクしてください。3ツールとも、必要になるまで本文を読み込まない仕組み（progressive disclosure）で動きます。

## 検証方法

構成を変えたあとは、各ツールでスキルが見えているか確認できます。

```bash
# Claude Code
claude -p "利用可能な skill 名を全部列挙して"

# Antigravity
agy --add-dir "$PWD" --print='利用可能な skill を名前とパスで列挙して'

# Codex
codex exec --sandbox read-only "利用可能な skill を名前とパスで列挙して"
```

Antigravity CLI（`agy`）は devcontainer のイメージに含まれています。
`claude` と `codex` は VSCode 拡張（`Anthropic.claude-code` / `openai.chatgpt`）に同梱されたバイナリなので、PATH が通っていないことがあります。その場合は拡張のインストール先を直接指定してください。

```bash
ls ~/.vscode-server/extensions/openai.chatgpt-*/bin/*/codex
```
