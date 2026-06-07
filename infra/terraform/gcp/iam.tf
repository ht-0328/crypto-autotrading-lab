# Cloud Build Service Account Permissions

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

# Cloud Run Runtime Service Account Permissions

resource "google_storage_bucket_iam_member" "runtime_sa_object_admin" {
  bucket = google_storage_bucket.app_bucket.name
  role   = "roles/storage.objectAdmin"
  member = "serviceAccount:${google_service_account.runtime_sa.email}"
}

# Cloud Scheduler Service Account Permissions

# Explicitly grant run.invoker to the Scheduler SA. Since this is for a specific job, it can also be
# done on the job itself, but following the design doc we ensure the SA has the right role.
# We will grant it at the project level for run jobs or on the job directly in cloud-run-job.tf if needed.
# For broader access, we can use project iam, but job-level is safer. We will use project level here
# if not easily applied to job, but cloud_run_v2_job_iam_member is preferred.
# Here we use project-level invoker for simplicity if job is not yet created, or better on the job itself later.
# Wait, the spec says "roles/run.invoker" -> "Cloud Run Job単位で付与".
# We will handle it in cloud-run-job.tf to bind to the specific job.

# Deploy Service Account Permissions

# GitHub Actions deployer needs to impersonate Build SA and Runtime SA
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
