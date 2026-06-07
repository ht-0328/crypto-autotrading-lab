variable "project_id" {
  description = "GCP Project ID"
  type        = string
}

variable "region" {
  description = "GCP Region"
  type        = string
  default     = "asia-northeast1"
}

variable "state_bucket_name" {
  description = "GCS Bucket name for Terraform state"
  type        = string
}
