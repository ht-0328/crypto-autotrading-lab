# インフラ設計 (Infrastructure)

このディレクトリには、GCP インフラをコードで構築するための「設計情報」が含まれています。
実際の操作手順（GitHub Actions の実行やデプロイ作業）については、[GCP 運用・デプロイガイド](../../operations/gcp/README.md) を参照してください。

## ドキュメント一覧

- [GCP インフラコード設計書](development-policy.md): Terraform などで GCP インフラをコード化するための設計。全体構成、リソース定義、IAM 方針、現状との差を扱います。

!!! note "Terraform の現状"

    Terraform の実装ファイルは [infra/terraform/gcp/](https://github.com/ht-0328/crypto-autotrading-lab/tree/main/infra/terraform/gcp/) に追加済みです。ただし現時点では `terraform apply` を運用しておらず、GCP リソースの構築とデプロイは GitHub Actions の `gcloud` コマンドが正です。一本化は今後の課題として [改善計画のバックログ](../../improvements/backlog.md) に登録しています。
