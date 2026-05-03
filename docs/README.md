# ドキュメント一覧

## 概要

このディレクトリ（`docs/`）には、本プロジェクトに関するすべてのドキュメントが格納されています。
ドキュメントは、対象読者に応じて「人間向け（`docs/human/`）」と「AIエージェント向け（`docs/ai/`）」に分類されています。

## docs/human と docs/ai の分類表

| ディレクトリ | 対象読者 | 目的 |
|---|---|---|
| `docs/human/` | 開発者、運用者 | プロダクトの仕様、開発手順、デプロイフローなどの理解 |
| `docs/ai/` | AIエージェント (Jules, Codex 等) | コード生成やレビュー時の制約、ルール、期待される挙動の定義 |

## 初めて読む場合の順番

初めてこのプロジェクトに参加する人間（開発者）は、以下の順番でドキュメントを読むことを推奨します。

1. [`../README.md`](../README.md) (リポジトリの全体像)
2. [`human/product-requirements.md`](human/product-requirements.md) (共通要求仕様)
3. [`human/phase1.md`](human/phase1.md) (現在のフェーズの仕様)
4. [`human/development.md`](human/development.md) (ローカル開発環境のセットアップ)
5. [`human/development-flow.md`](human/development-flow.md) (日々の開発フロー)

## GCPデプロイを行う場合の読む順番

GCPへのデプロイや環境構築を行う場合は、以下の順番で確認してください。

1. [`human/gcp-account-and-project-setup.md`](human/gcp-account-and-project-setup.md) (GCPの初期設定)
2. [`human/github-actions-gcp-deploy-setup.md`](human/github-actions-gcp-deploy-setup.md) (GitHub Actionsとの連携設定)
3. [`human/gcp-deployment.md`](human/gcp-deployment.md) (実際のデプロイと運用手順)

## AIエージェントに作業させる場合の読む順番

AIエージェントに開発を依頼する前に、人間が内容を把握しておくべきルールの順番です。

1. [`../AGENTS.md`](../AGENTS.md) (エージェントへの全体的な指示とコア・ルール)
2. [`ai/agents-guidelines.md`](ai/agents-guidelines.md) (AIの禁止事項・必須チェック項目)
3. [`ai/change-granularity.md`](ai/change-granularity.md) (PRの粒度に関するルール)

## 主要リンク

- [プロジェクトルートの README](../README.md)
- [AIエージェント用コアルール (AGENTS.md)](../AGENTS.md)
- [人間向けドキュメント一覧](human/README.md)
- [AI向けドキュメント一覧](ai/README.md)

## ドキュメントリンク方針

リポジトリ内のドキュメントを更新・作成する際は、以下のリンク方針を遵守してください。

- **リポジトリ内の文書参照はMarkdownリンクにする**
  - 単なるファイル名（例: `phase1.md`）として記述せず、必ず `[phase1.md](phase1.md)` のようにリンク化してください。
- **ファイル名だけを文字列で書かない**
  - コードブロック内などを除き、文中で他のドキュメントに言及する場合はリンクを使用してください。
- **相対パスでリンクする**
  - リポジトリの構造変更に強くなるよう、絶対パスではなく相対パス（例: `../human/phase1.md`）を使用してください。
- **外部URLには目的が分かるラベルを付ける**
  - 直接 `https://...` と書くのではなく、`[GCP公式ドキュメント](https://...)` のようにラベルを付けてください。
- **リンク切れを増やさない**
  - ファイル名の変更や移動を行った際は、必ず関連するドキュメント内のリンクも修正してください。
