# GCP インフラコード設計書

このディレクトリは「インフラ設計書」の置き場です。

`docs/operations/gcp` は運用手順（GitHub Actions の実行手順やデプロイ作業など）のためのドキュメントですが、`docs/infrastructure/gcp` はインフラの設計情報そのものを扱います。

最初に読むべきファイルである `development-policy.md` は、TerraformなどでGCPインフラコードを書く前提となる設計書です。

Terraform の実装ファイルは [infra/terraform/gcp/](https://github.com/ht-0328/crypto-autotrading-lab/tree/main/infra/terraform/gcp/) に追加済みです。ただし現時点では `terraform apply` を運用しておらず、GCP リソースの構築とデプロイは GitHub Actions の `gcloud` コマンドが正です。一本化は今後の課題として [改善計画のバックログ](../../improvements/backlog.md) に登録しています。

運用手順や具体的なデプロイ方法を探す場合は、`docs/operations/gcp` を参照してください。
