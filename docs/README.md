# ドキュメント一覧

## 概要

このディレクトリ（`docs/`）には、本プロジェクトに関するすべてのドキュメントが格納されています。
読む目的に合わせて、必要なフォルダのドキュメントを参照してください。

## 目的別の案内（どこから読めばよいか）

### 🔰 初めてこのプロジェクトに参加する方
まずはプロジェクトの目的や全体像を把握してください。
1. [リポジトリの全体像](../README.md)
2. [アプリの目的と一番大事な方針 (overview/product.md)](overview/product.md)
3. [Phase1 の仕様と全体像 (architecture/phase1-overview.md)](architecture/phase1-overview.md)

### 📈 アプリの目的やロードマップを知りたい方
1. [アプリの目的と一番大事な方針 (overview/product.md)](overview/product.md)
2. [ロードマップと完了条件 (overview/roadmap.md)](overview/roadmap.md)

### 🧠 売買ロジックの仕組みを知りたい方
1. [売買ロジック説明 (architecture/trading-logic.md)](architecture/trading-logic.md)

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

### 🤖 JulesやCodex（AI）に作業させたい方
AIエージェントに開発を依頼する前に、人間が内容を把握しておくべきルールです。
1. [AIエージェント用コアルール (../AGENTS.md)](../AGENTS.md)
2. [AIの禁止事項・必須チェック項目 (ai/agents-guidelines.md)](ai/agents-guidelines.md)
3. [PRの粒度に関するルール (ai/change-granularity.md)](ai/change-granularity.md)
4. AI向けドキュメントの一覧は [こちら (ai/README.md)](ai/README.md) を参照。

## ディレクトリ構成

- `overview/`: プロダクトの目的やロードマップ
- `architecture/`: システムの構造や売買ロジックの仕様
- `development/`: 開発環境の構築手順や日々の開発フロー
- `operations/`: GCP環境の構築やデプロイ手順
- `ai/`: AIエージェント向けのルールや制約事項

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
