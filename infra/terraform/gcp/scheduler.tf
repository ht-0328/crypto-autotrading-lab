# 定期実行を行う Cloud Scheduler の定義

# 指定したスケジュール（Cron）で Cloud Run Job を起動するジョブを作成する
resource "google_cloud_scheduler_job" "job" {
  name             = var.scheduler_job_name
  description      = "Trigger for Cloud Run Job"
  schedule         = var.scheduler_cron
  time_zone        = var.scheduler_time_zone
  region           = var.region

  http_target {
    http_method = "POST"
    uri         = "https://run.googleapis.com/v2/projects/${var.project_id}/locations/${var.region}/jobs/${google_cloud_run_v2_job.app_job.name}:run"

    oauth_token {
      # この Service Account を使って認証し、Cloud Run Job を起動する
      service_account_email = google_service_account.scheduler_sa.email
    }
  }

  depends_on = [
    google_project_service.enabled_apis
  ]
}
