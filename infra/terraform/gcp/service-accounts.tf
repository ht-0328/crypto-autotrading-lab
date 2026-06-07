resource "google_service_account" "build_sa" {
  account_id   = var.build_service_account_name
  display_name = "Service Account for Cloud Build"
  description  = "Service Account to build and push docker images"

  depends_on = [
    google_project_service.enabled_apis
  ]
}

resource "google_service_account" "runtime_sa" {
  account_id   = var.runtime_service_account_name
  display_name = "Service Account for Cloud Run Job Runner"
  description  = "Service Account to execute Cloud Run Job"

  depends_on = [
    google_project_service.enabled_apis
  ]
}

resource "google_service_account" "scheduler_sa" {
  account_id   = var.scheduler_service_account_name
  display_name = "Service Account for Cloud Scheduler Invoker"
  description  = "Service Account to trigger Cloud Run Jobs"

  depends_on = [
    google_project_service.enabled_apis
  ]
}
