# Terraform 本体および Provider のバージョン・Backend 設定

terraform {
  required_version = ">= 1.5.0"

  # Backend として GCS を使用する（バケット名は init 時に backend-config で渡す）
  backend "gcs" {
    prefix = "terraform/gcp"
  }

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 6.0"
    }
  }
}
