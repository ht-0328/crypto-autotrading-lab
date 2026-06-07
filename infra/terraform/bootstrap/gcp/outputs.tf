# 出力値の定義

output "state_bucket_name" {
  description = "作成された Terraform state 用 GCS Bucket の名前"
  value       = google_storage_bucket.terraform_state.name
}
