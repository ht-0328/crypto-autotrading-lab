# プロジェクト内で利用する Service Account の定義

# GitHub Actions が Docker イメージをビルド・プッシュするために使用する Cloud Build 用 Service Account を作成する
resource "google_service_account" "build_sa" {
  account_id   = var.build_service_account_name
  display_name = "Service Account for Cloud Build"
  description  = "Docker イメージのビルドとプッシュを行う Service Account"

  depends_on = [
    google_project_service.enabled_apis
  ]
}

# Cloud Run Job の実行主体となる Runtime 用 Service Account を作成する
resource "google_service_account" "runtime_sa" {
  account_id   = var.runtime_service_account_name
  display_name = "Service Account for Cloud Run Job Runner"
  description  = "Cloud Run Job の実行主体となる Service Account"

  depends_on = [
    google_project_service.enabled_apis
  ]
}

# 定期実行トリガーをかける Cloud Scheduler 用 Service Account を作成する
resource "google_service_account" "scheduler_sa" {
  account_id   = var.scheduler_service_account_name
  display_name = "Service Account for Cloud Scheduler Invoker"
  description  = "Cloud Run Job を起動する Service Account"

  depends_on = [
    google_project_service.enabled_apis
  ]
}
