# Terraform State 管理用 GCS Bucket の定義

# Terraform state を保存するための GCS Bucket を作成する
resource "google_storage_bucket" "terraform_state" {
  name          = var.state_bucket_name
  location      = var.region
  force_destroy = false

  # terraform.tfstate の誤更新・破損時に復旧できるようバージョニングを有効化する
  versioning {
    enabled = true
  }

  uniform_bucket_level_access = true

  # 外部から追加される可能性のあるライフサイクルルール等の変更を無視し、誤削除を防止する
  lifecycle {
    prevent_destroy = true
  }
}
