# 外部から渡す変数の定義

variable "project_id" {
  description = "GCP プロジェクトID"
  type        = string
}

variable "region" {
  description = "GCP リージョン"
  type        = string
  default     = "asia-northeast1"
}

variable "state_bucket_name" {
  description = "Terraform state を保存する GCS Bucket 名"
  type        = string
}
