# 人間向けドキュメント一覧

## 概要

このディレクトリ（`docs/human/`）には、人間の開発者および運用者が参照するためのプロジェクト仕様書、開発フロー、デプロイ手順などが格納されています。

## ドキュメント一覧

| ドキュメント | 目的 | 読むタイミング | 関連する作業 |
|---|---|---|---|
| [`product-requirements.md`](product-requirements.md) | プロダクト全体の共通要求仕様、KPI、非機能要件、セキュリティ境界の定義 | プロジェクト参加時、全体像の把握時 | 仕様設計、アーキテクチャ設計 |
| [`phase1.md`](phase1.md) | 現在のフェーズにおける具体的な仕様、ディレクトリ構成、実行フローの定義 | Phase1の実装着手時 | アプリケーション開発、テスト作成 |
| [`trading-logic.md`](trading-logic.md) | 現在の売買判定ロジックの初心者向け解説、各Strategyの違いと判断基準 | ロジックの仕組みを把握したい時 | パラメータ調整、ロジック変更 |
| [`roadmap.md`](roadmap.md) | 各フェーズの着手・完了条件、未解決リスク、禁止事項の管理 | 次フェーズへの移行判断時 | プロジェクト計画、タスク選定 |
| [`development-flow.md`](development-flow.md) | 日々のタスク消化、PR作成、レビュー、マージのフローの定義 | 開発タスクの着手前 | コーディング、コードレビュー |
| [`development.md`](development.md) | ローカル開発環境（DevContainer）のセットアップ、起動、テスト手順 | 環境構築時 | ローカルでの動作確認、テスト実行 |
| [`gcp-account-and-project-setup.md`](gcp-account-and-project-setup.md) | GCPの初期アカウント作成とプロジェクト設定の手順 | GCP利用の初回セットアップ時 | GCPプロジェクト作成、課金設定 |
| [`github-actions-gcp-deploy-setup.md`](github-actions-gcp-deploy-setup.md) | GitHub ActionsからGCPへデプロイするための認証と権限の準備 | CI/CDパイプライン構築時 | Workload Identity Federation設定 |
| [`gcp-deployment.md`](gcp-deployment.md) | GCP環境（Cloud Run, Cloud Scheduler等）へのデプロイと運用手順 | デプロイ実行時、運用管理時 | クラウドデプロイ、定期実行設定 |
