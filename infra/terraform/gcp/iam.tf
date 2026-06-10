# Cloud Build Service Account への権限付与

resource "google_artifact_registry_repository_iam_member" "build_sa_writer" {
  project    = google_artifact_registry_repository.repo.project
  location   = google_artifact_registry_repository.repo.location
  repository = google_artifact_registry_repository.repo.name
  role       = "roles/artifactregistry.writer"
  member     = "serviceAccount:${google_service_account.build_sa.email}"
}

resource "google_project_iam_member" "build_sa_log_writer" {
  project = var.project_id
  role    = "roles/logging.logWriter"
  member  = "serviceAccount:${google_service_account.build_sa.email}"
}

resource "google_project_iam_member" "build_sa_object_viewer" {
  project = var.project_id
  role    = "roles/storage.objectViewer"
  member  = "serviceAccount:${google_service_account.build_sa.email}"
}

# Cloud Run Runtime Service Account への権限付与

resource "google_storage_bucket_iam_member" "runtime_sa_object_admin" {
  bucket = google_storage_bucket.app_bucket.name
  role   = "roles/storage.objectAdmin"
  member = "serviceAccount:${google_service_account.runtime_sa.email}"
}

# Deploy Service Account への権限付与

# GitHub Actions deployer が Cloud Build SA と Cloud Run Runtime SA を利用できるようにする
resource "google_service_account_iam_member" "deploy_sa_user_build" {
  service_account_id = google_service_account.build_sa.name
  role               = "roles/iam.serviceAccountUser"
  member             = "serviceAccount:${var.deploy_service_account_email}"
}

resource "google_service_account_iam_member" "deploy_sa_user_runtime" {
  service_account_id = google_service_account.runtime_sa.name
  role               = "roles/iam.serviceAccountUser"
  member             = "serviceAccount:${var.deploy_service_account_email}"
}
