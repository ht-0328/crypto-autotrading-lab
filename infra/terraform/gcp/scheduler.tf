resource "google_cloud_scheduler_job" "job" {
  name        = var.scheduler_job_name
  description = "Trigger for Cloud Run Job"
  schedule    = var.scheduler_cron
  time_zone   = var.scheduler_time_zone
  region      = var.region

  http_target {
    http_method = "POST"
    uri         = "https://${var.region}-run.googleapis.com/apis/run.googleapis.com/v1/namespaces/${var.project_id}/jobs/${google_cloud_run_v2_job.app_job.name}:run"

    oauth_token {
      service_account_email = google_service_account.scheduler_sa.email
    }
  }

  depends_on = [
    google_project_service.enabled_apis
  ]
}
