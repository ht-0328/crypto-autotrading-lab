# インフラ設計 (Infrastructure)

| 項目 | 内容 |
| --- | --- |
| 想定読者 | GCP インフラをコードで定義・変更する開発者 |
| 読んだあとできること | インフラの設計と、実際の操作手順のどちらを読めばよいかを判断できる |
| 状態 | 現行 |
| 機密区分 | 公開可 |
| 作成者 | リポジトリ管理者 |
| 保守責任者 | リポジトリ管理者 |
| 最終確認日 | 2026-08-30 |

このディレクトリには、GCP インフラをコードで構築するための「設計情報」が含まれています。
実際の操作手順は [GCP 運用・デプロイガイド](../../operations/gcp/README.md) にあります。GitHub Actions の実行やデプロイ作業はそちらです。

## ドキュメント一覧

- [GCP インフラコード設計書](development-policy.md): インフラをコード化するための設計。
  - 全体構成、リソース定義、IAM 方針、現状との差を扱います。

!!! note "Terraform の現状"

    Terraform の実装ファイルは [infra/terraform/gcp/](https://github.com/ht-0328/crypto-autotrading-lab/tree/main/infra/terraform/gcp/) に追加済みです。
    ただし 2026-08-30 時点で `terraform apply` は運用していません。
    GCP リソースの構築とデプロイは、GitHub Actions の `gcloud` コマンドが正です。
    一本化の判断は [改善計画のバックログ](../../improvements/backlog.md) に登録しています。
