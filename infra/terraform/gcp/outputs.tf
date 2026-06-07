output "artifact_registry_repo" {
  description = "The ID of the Artifact Registry repository"
  value       = google_artifact_registry_repository.repo.id
}

output "gcs_bucket_name" {
  description = "The name of the app GCS bucket"
  value       = google_storage_bucket.app_bucket.name
}

output "cloud_run_job_name" {
  description = "The name of the Cloud Run Job"
  value       = google_cloud_run_v2_job.app_job.name
}
