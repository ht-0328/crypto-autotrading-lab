resource "google_storage_bucket" "app_bucket" {
  name          = var.gcs_bucket_name
  location      = var.region
  force_destroy = false

  uniform_bucket_level_access = true

  depends_on = [
    google_project_service.enabled_apis
  ]
}
