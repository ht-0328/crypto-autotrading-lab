# ドキュメント一覧

## 概要

このディレクトリ（`docs/`）には、本プロジェクトに関するすべてのドキュメントが格納されています。
読む目的に合わせて、必要なフォルダのドキュメントを参照してください。

## 目的別の案内（どこから読めばよいか）

### 🔰 初めてこのプロジェクトに参加する方

まずはプロジェクトの目的や全体像を把握してください。

1. [リポジトリの全体像](../README.md)
2. [アプリの目的と一番大事な方針 (overview/product.md)](overview/product.md)
3. [Phase1 の仕様と全体像 (specifications/phase1-simulation.md)](specifications/phase1-simulation.md)
4. [システム全体構成 設計書 (architecture/system-overview.md)](architecture/system-overview.md)

### 📈 アプリの目的やロードマップを知りたい方

1. [アプリの目的と一番大事な方針 (overview/product.md)](overview/product.md)
2. [ロードマップと完了条件 (overview/roadmap.md)](overview/roadmap.md)

> **リアル注文について**: [リアル購入処理の仕様 (specifications/features/real-trading-gmo-order.md)](specifications/features/real-trading-gmo-order.md) は **Phase3** のスコープです。コードは先行実装されていますが、Phase1 では起動時ガードにより実行できません。

### 🧠 売買ロジックの仕組みを知りたい方

1. [売買ロジック説明 (architecture/trading-logic.md)](architecture/trading-logic.md)
2. [過去K線CSV作成機能の仕様 (specifications/features/kline-csv-export.md)](specifications/features/kline-csv-export.md)
3. [過去K線CSV読み込み機能の仕様 (specifications/features/kline-csv-import.md)](specifications/features/kline-csv-import.md)
4. [バックテスト機能の仕様 (specifications/features/backtest.md)](specifications/features/backtest.md)

### 💻 ローカルで開発・テストをしたい方

1. [開発環境のセットアップ (development/setup.md)](development/setup.md)
2. [日々の開発フローとルール (development/workflow.md)](development/workflow.md)

### ☁️ GCPへデプロイやGitHub Actionsの設定をしたい方

GCPへのデプロイ準備は、以下の順番で進めてください。

1. [GCP アカウントとプロジェクトのセットアップ (operations/gcp/01-account-and-project.md)](operations/gcp/01-account-and-project.md)
2. [gcloud CLI の準備とログイン (operations/gcp/02-gcloud-cli.md)](operations/gcp/02-gcloud-cli.md)
3. [Workload Identity Federation の設定 (operations/gcp/03-workload-identity-federation.md)](operations/gcp/03-workload-identity-federation.md)
4. [サービスアカウントの作成とIAM設定 (operations/gcp/04-service-accounts-and-iam.md)](operations/gcp/04-service-accounts-and-iam.md)
5. [GitHub Actions Variables の設定 (operations/gcp/05-github-actions-variables.md)](operations/gcp/05-github-actions-variables.md)
6. [Cloud Run Job へのデプロイ (operations/gcp/06-deploy-cloud-run-job.md)](operations/gcp/06-deploy-cloud-run-job.md)
7. [Cloud Scheduler による定期実行設定 (operations/gcp/07-scheduler.md)](operations/gcp/07-scheduler.md)
8. （不要になった場合）[リソースのクリーンアップ (operations/gcp/08-cleanup.md)](operations/gcp/08-cleanup.md)

### 🚀 実注文（自動売買）に向けて進めたい方

実資金での自動売買に到達するまでの作業計画です。**現在のコードのまま実注文を有効にしてはいけない理由**も、ここに書いてあります。

1. [実注文までの作業計画 (plans/README.md)](plans/README.md)
2. [実注文を始めるまでの手順 (operations/go-live-runbook.md)](operations/go-live-runbook.md) — オーナー本人にしかできない作業

### 🛠 既知の課題を直したい方

洗い出し済みの改善項目です。1ファイル = 1PR の単位に分かれており、ファイルのパスを指定すればその作業だけを実施できます。

1. [改善計画の一覧 (improvements/README.md)](improvements/README.md)
2. [指摘一覧と根拠 (improvements/findings.md)](improvements/findings.md)
3. [第3波バックログ (improvements/backlog.md)](improvements/backlog.md)

### 🤖 AI（Claude Code / Codex / Antigravity）に作業させたい方

AIエージェントに開発を依頼する前に、人間が内容を把握しておくべきルールです。

1. [AIエージェント用の共通ルール (../AGENTS.md)](../AGENTS.md)
2. [作業別の詳細ルール（スキル） (../.agents/skills/)](../.agents/skills/)
3. [3ツール共用の仕組みと編集方法 (../.agents/README.md)](../.agents/README.md)

## ディレクトリ構成

- **`specifications/`**: 仕様書を置く場所です。何を満たす必要があるか、入力・出力・条件・エラー・具体例などを定義しています。仕様を確認したい場合はここを参照してください。
- **`architecture/`**: 設計書を置く場所です。どのように作るか、責務分担・処理の流れ・クラス配置・データ保存などを定義しています。設計を確認したい場合はここを参照してください。
- **`templates/`**: 仕様書および設計書を作成する際のテンプレートを置いています。新しく文書を書く場合はここを見てください。
- `overview/`: プロダクトの目的やロードマップ
- `development/`: 開発環境の構築手順や日々の開発フロー
- `operations/`: GCP環境の構築やデプロイ手順
- **`plans/`**: 実注文（自動売買）に到達するまでの作業計画です。着手順と、実資金を投入してよい条件を定義しています。
- **`improvements/`**: 仕様・ドキュメント・インフラ・実装の食い違いを洗い出した結果と、その解消計画です。1ファイル = 1PR の単位で分けてあり、ファイルを指定すればその作業だけを実施できます。

## ドキュメントリンク方針

リポジトリ内のドキュメントを更新・作成する際は、以下のリンク方針を遵守してください。

- **リポジトリ内の文書参照はMarkdownリンクにする**
  - 単なるファイル名（例: `phase1-overview.md`）として記述せず、必ず `[phase1-overview.md](phase1-overview.md)` のようにリンク化してください。
- **ファイル名だけを文字列で書かない**
  - コードブロック内などを除き、文中で他のドキュメントに言及する場合はリンクを使用してください。
- **相対パスでリンクする**
  - リポジトリの構造変更に強くなるよう、絶対パスではなく相対パス（例: `../development/setup.md`）を使用してください。
- **外部URLには目的が分かるラベルを付ける**
  - 直接 `https://...` と書くのではなく、`[GCP公式ドキュメント](https://...)` のようにラベルを付けてください。
- **リンク切れを増やさない**
  - ファイル名の変更や移動を行った際は、必ず関連するドキュメント内のリンクも修正してください。
