# 機密情報を管理する Secret Manager リソースの定義

# API キーなどの機密情報を保存するための Secret Manager の「入れ物」を作成する（実値は含まない）
resource "google_secret_manager_secret" "secrets" {
  for_each  = toset(var.secret_names)
  secret_id = each.key

  replication {
    auto {}
  }

  depends_on = [
    google_project_service.enabled_apis
  ]
}

# Cloud Run Runtime Service Account に対して、Secret の読み取り権限を付与する
resource "google_secret_manager_secret_iam_member" "runtime_sa_secret_accessor" {
  for_each  = google_secret_manager_secret.secrets
  secret_id = each.value.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.runtime_sa.email}"
}
