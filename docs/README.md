# ドキュメント一覧

| 項目 | 内容 |
| --- | --- |
| 想定読者 | このリポジトリのドキュメントを読む人すべて |
| 読んだあとできること | 自分の目的に合った文書へ、最短で辿り着ける |
| 状態 | 現行 |
| 機密区分 | 公開可 |
| 作成者 | リポジトリ管理者 |
| 保守責任者 | リポジトリ管理者 |
| 最終確認日 | 2026-08-30 |

## 概要

このディレクトリ（`docs/`）には、本プロジェクトに関するすべてのドキュメントが格納されています。
読む目的に合わせて、必要なフォルダのドキュメントを参照してください。

この内容は [Zensical](https://zensical.org/) で静的サイト化し、[GitHub Pages](https://ht-0328.github.io/crypto-autotrading-lab/) で公開しています。手元での確認方法とサイトの設定は [開発環境のセットアップ](development/setup.md) を参照してください。

## 目的別の案内（どこから読めばよいか）

読む目的ごとに、辿る順番を示します。上から順に読む必要はありません。

### 🔰 初めてこのプロジェクトに参加する方

まずはプロジェクトの目的や全体像を把握してください。

1. [リポジトリの全体像](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/README.md)
2. [アプリの目的と一番大事な方針](overview/product.md)
3. [Phase1 の仕様と全体像](specifications/phase1-simulation.md)
4. [システム全体構成 設計書](architecture/system-overview.md)

### 📈 アプリの目的やロードマップを知りたい方

1. [アプリの目的と一番大事な方針](overview/product.md)
2. [ロードマップと完了条件](overview/roadmap.md)

!!! warning "リアル注文について"

    [リアル購入処理の仕様](specifications/features/real-trading-gmo-order.md) は **Phase3** のスコープです。コードは先行実装されていますが、Phase1 では起動時ガードにより実行できません。

### 🧠 売買ロジックの仕組みを知りたい方

1. [売買ロジック説明](architecture/trading-logic.md)
2. [過去K線CSV作成機能の仕様](specifications/features/kline-csv-export.md)
3. [過去K線CSV読み込み機能の仕様](specifications/features/kline-csv-import.md)
4. [バックテスト機能の仕様](specifications/features/backtest.md)

### 💻 ローカルで開発・テストをしたい方

1. [開発環境のセットアップ](development/setup.md)
2. [日々の開発フローとルール](development/workflow.md)

### ☁️ GCPへデプロイやGitHub Actionsの設定をしたい方

GCPへのデプロイ準備は、以下の順番で進めてください。

**環境と認証を用意する**

1. [GCP アカウントとプロジェクトのセットアップ](operations/gcp/01-account-and-project.md)
2. [gcloud CLI の準備とログイン](operations/gcp/02-gcloud-cli.md)
3. [Workload Identity Federation の設定](operations/gcp/03-workload-identity-federation.md)
4. [サービスアカウントの作成とIAM設定](operations/gcp/04-service-accounts-and-iam.md)

**デプロイして動かす**

5. [GitHub Actions Variables の設定](operations/gcp/05-github-actions-variables.md)
6. [Cloud Run Job へのデプロイ](operations/gcp/06-deploy-cloud-run-job.md)
7. [Cloud Scheduler による定期実行設定](operations/gcp/07-scheduler.md)

不要になった場合は [リソースのクリーンアップ](operations/gcp/08-cleanup.md) に進みます。

### 🚀 実注文（自動売買）に向けて進めたい方

実資金での自動売買に到達するまでの作業計画です。**現在のコードのまま実注文を有効にしてはいけない理由**も、ここに書いてあります。

1. [実注文までの作業計画](plans/README.md)
2. [実注文を始めるまでの手順](operations/go-live-runbook.md) — オーナー本人にしかできない作業

### 🛠 既知の課題を直したい方

洗い出し済みの改善項目です。1ファイル = 1PR の単位に分かれており、ファイルのパスを指定すればその作業だけを実施できます。

1. [改善計画の一覧](improvements/README.md)
2. [指摘一覧と根拠](improvements/findings.md)
3. [第3波バックログ](improvements/backlog.md)

### 🤖 AI（Claude Code / Codex / Antigravity）に作業させたい方

AIエージェントに開発を依頼する前に、人間が内容を把握しておくべきルールです。

1. [AIエージェント用の共通ルール](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/AGENTS.md)
2. [作業別の詳細ルール（スキル） (../.agents/skills/)](https://github.com/ht-0328/crypto-autotrading-lab/tree/main/.agents/skills/)
3. [3ツール共用の仕組みと編集方法](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/.agents/README.md)

## ディレクトリ構成

各ディレクトリの役割は次のとおりです。仕様（何を満たすか）と設計（どう作るか）を分けて置いています。

| ディレクトリ | 置くもの |
| --- | --- |
| `specifications/` | 仕様書。入力・出力・条件・エラー・具体例を定義する |
| `architecture/` | 設計書。責務分担・処理の流れ・クラス配置・データ保存を定義する |
| `overview/` | プロダクトの目的とロードマップ |
| `development/` | 開発環境の構築手順と、日々の開発フロー |
| `operations/` | GCP環境の構築、デプロイ、復旧の手順 |
| `infrastructure/` | GCP インフラをコードで構築するための設計 |
| `plans/` | 実注文に到達するまでの作業計画。着手順と、実資金を入れてよい条件 |
| `improvements/` | 仕様・文書・インフラ・実装の食い違いと、その解消計画 |
| `templates/` | 仕様書・設計書を新しく書くときのテンプレート |

`plans/` と `improvements/` は、1ファイル = 1PR の単位に分けてあります。ファイルのパスを指定すれば、その作業だけを実施できます。

## ドキュメントリンク方針

リポジトリ内のドキュメントを更新・作成する際は、以下のリンク方針を遵守してください。

- **リポジトリ内の文書参照はMarkdownリンクにする**
  - 単なるファイル名として書かず、必ずリンクにしてください。

    ```markdown
    悪い例: 詳しくは phase1-simulation.md を参照してください。
    良い例: 詳しくは [Phase1 仕様書](specifications/phase1-simulation.md) を参照してください。
    ```
- **ファイル名だけを文字列で書かない**
  - コードブロック内などを除き、文中で他のドキュメントに言及する場合はリンクを使用してください。
- **`docs/` の中は相対パスでリンクする**
  - リポジトリの構造が変わっても壊れにくいよう、絶対パスではなく相対パスを使ってください。例は `../development/setup.md` です。
- **`docs/` の外は GitHub の絶対URLでリンクする**
  - 公開サイト（[GitHub Pages](https://ht-0328.github.io/crypto-autotrading-lab/)）がビルドするのは `docs/` 配下だけです。
  - `docs/` の外を `../../projects/...` のような相対パスで参照すると、**サイト上ではリンク切れになります**。
  - 対象は Kotlin ソース・ワークフロー・Terraform・`AGENTS.md`・`.agents/` です。
  - ファイルは `https://github.com/ht-0328/crypto-autotrading-lab/blob/main/<パス>`、ディレクトリは `blob` の代わりに `tree` を使ってください。
  - 例: `[TradingApplication](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/application/TradingApplication.kt)`
- **リンクのラベルにパスを書かない**
  - `[ロードマップ](overview/roadmap.md)` と書きます。`[ロードマップ (overview/roadmap.md)](overview/roadmap.md)` のようにパスを重ねません。
  - ラベルはリンク先が分かる言葉にします。「こちら」「ここ」は使いません。
- **外部URLには目的が分かるラベルを付ける**
  - 直接 `https://...` と書くのではなく、`[GCP公式ドキュメント](https://...)` のようにラベルを付けてください。
- **リンク切れを増やさない**
  - ファイル名の変更や移動を行った際は、必ず関連するドキュメント内のリンクも修正してください。
  - コミット前に `python3 scripts/check-doc-links.py` を実行してください。`docs/` の外を指す相対リンクが残っていると、検出されて終了コード 1 を返します。
- **ファイルを追加・移動・削除したら `nav` も更新する**
  - 公開サイトの目次は [zensical.toml](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/zensical.toml) の `nav` が決めています。`nav` に無いファイルはビルドはされますが、**サイドバーから辿れません**。
  - 手順は [開発環境のセットアップ](development/setup.md) の「章立てを変えるとき」を参照してください。
