resource "google_storage_bucket" "terraform_state" {
  name          = var.state_bucket_name
  location      = var.region
  force_destroy = false

  # terraform.tfstate のバックアップ/復旧のためにバージョニングを有効化する
  versioning {
    enabled = true
  }

  uniform_bucket_level_access = true

  # 外部から追加される可能性のあるライフサイクルルール等の変更を無視し、誤削除を防止する
  lifecycle {
    prevent_destroy = true
  }
}
