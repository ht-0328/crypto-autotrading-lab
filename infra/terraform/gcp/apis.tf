# プロジェクトで利用する GCP API の有効化設定

locals {
  services = [
    "serviceusage.googleapis.com",
    "iam.googleapis.com",
    "iamcredentials.googleapis.com",
    "cloudbuild.googleapis.com",
    "artifactregistry.googleapis.com",
    "run.googleapis.com",
    "storage.googleapis.com",
    "secretmanager.googleapis.com",
    "cloudscheduler.googleapis.com"
  ]
}

# 必要な API を一括で有効化する
resource "google_project_service" "enabled_apis" {
  for_each = toset(local.services)
  project  = var.project_id
  service  = each.key

  disable_dependent_services = false
  disable_on_destroy         = false
}
