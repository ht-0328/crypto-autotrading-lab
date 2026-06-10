# 出力値の定義

output "artifact_registry_repo" {
  description = "作成された Artifact Registry リポジトリの ID"
  value       = google_artifact_registry_repository.repo.id
}

output "gcs_bucket_name" {
  description = "アプリケーションデータを保存する GCS Bucket の名前"
  value       = google_storage_bucket.app_bucket.name
}

output "cloud_run_job_name" {
  description = "作成された Cloud Run Job の名前"
  value       = google_cloud_run_v2_job.app_job.name
}
