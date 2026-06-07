resource "google_artifact_registry_repository" "repo" {
  location      = var.region
  repository_id = var.artifact_repository_name
  format        = "DOCKER"
  description   = "Docker repository for crypto autotrading app"

  depends_on = [
    google_project_service.enabled_apis
  ]
}
