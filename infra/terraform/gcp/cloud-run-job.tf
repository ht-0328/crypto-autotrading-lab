# アプリケーションを実行する Cloud Run Job の定義

# 売買ロジックを実行する Cloud Run Job を作成する
resource "google_cloud_run_v2_job" "app_job" {
  name     = var.cloud_run_job_name
  location = var.region

  template {
    template {
      max_retries = 0

      # 実行主体となる Service Account を指定
      service_account = google_service_account.runtime_sa.email

      # アプリケーションデータを読み書きする GCS バケットのマウント設定
      volumes {
        name = "gcs"
        gcs {
          bucket    = google_storage_bucket.app_bucket.name
          read_only = false
        }
      }

      containers {
        image = var.image_uri

        volume_mounts {
          name       = "gcs"
          mount_path = "/mnt/gcs"
        }

        # アプリケーションの設定値（環境変数）
        # 出力先は GCS マウント配下に揃える。未設定だとコンテナローカルに出力され、Job 終了時に消える
        env {
          name  = "APP_DATA_DIR"
          value = "/mnt/gcs/data"
        }
        env {
          name  = "APP_INTERVAL"
          value = var.app_interval
        }
        env {
          name  = "APP_TRADING_STRATEGY_NAME"
          value = var.app_trading_strategy_name
        }
        env {
          name  = "TRADING_SYMBOL"
          value = var.trading_symbol
        }
        env {
          name  = "TRADING_INITIAL_CAPITAL"
          value = var.trading_initial_capital
        }
        env {
          name  = "TRADING_TRADE_AMOUNT"
          value = var.trading_trade_amount
        }
        env {
          name  = "TRADING_BUY_THRESHOLD"
          value = var.trading_buy_threshold
        }
        env {
          name  = "TRADING_SELL_THRESHOLD"
          value = var.trading_sell_threshold
        }
        env {
          name  = "TRADING_VOLATILITY_THRESHOLD"
          value = var.trading_volatility_threshold
        }
        env {
          name  = "TRADING_SHARP_CHANGE_THRESHOLD"
          value = var.trading_sharp_change_threshold
        }
        env {
          name  = "TRADING_COOLDOWN_LENGTH"
          value = var.trading_cooldown_length
        }
        env {
          name  = "TRADING_ATR_LENGTH"
          value = var.trading_atr_length
        }
        env {
          name  = "TRADING_ATR_PROFIT_MULTIPLIER"
          value = var.trading_atr_profit_multiplier
        }
        env {
          name  = "TRADING_ATR_LOSS_MULTIPLIER"
          value = var.trading_atr_loss_multiplier
        }
        env {
          name  = "TRADING_ORDER_SIZING_MODE"
          value = var.trading_order_sizing_mode
        }
        env {
          name  = "API_RETRY_COUNT"
          value = var.api_retry_count
        }
        env {
          name  = "API_PUBLIC_BASE_URL"
          value = var.api_public_base_url
        }
        env {
          name  = "API_PRIVATE_BASE_URL"
          value = var.api_private_base_url
        }
        env {
          name  = "OUTPUT_PATH"
          value = var.output_path
        }
        env {
          name  = "STATE_PATH"
          value = var.state_path
        }

        # リアル取引の設定値（シミュレーションフェーズのデフォルトは安全側に倒す）
        env {
          name  = "REAL_TRADING_DRY_RUN"
          value = "true"
        }
        env {
          name  = "REAL_TRADING_ENABLED"
          value = "false"
        }
        env {
          name  = "REAL_TRADING_STOP_ON_UNCONFIRMED_ORDER"
          value = var.real_trading_stop_on_unconfirmed_order
        }
        env {
          name  = "REAL_TRADING_MAX_ORDER_JPY"
          value = var.real_trading_max_order_jpy
        }
        env {
          name  = "REAL_TRADING_MAX_DAILY_ORDER_JPY"
          value = var.real_trading_max_daily_order_jpy
        }
        env {
          name  = "REAL_TRADING_MAX_POSITION_JPY"
          value = var.real_trading_max_position_jpy
        }

        # Secret Manager 参照経由での機密情報の取得
        env {
          name = "GMO_API_KEY"
          value_source {
            secret_key_ref {
              secret  = google_secret_manager_secret.secrets["gmo-api-key"].secret_id
              version = "latest"
            }
          }
        }
        env {
          name = "GMO_API_SECRET"
          value_source {
            secret_key_ref {
              secret  = google_secret_manager_secret.secrets["gmo-api-secret"].secret_id
              version = "latest"
            }
          }
        }
      }
    }
  }

  depends_on = [
    google_project_service.enabled_apis
  ]
}

# Cloud Scheduler SA に対して、この Cloud Run Job を起動する権限を付与する
resource "google_cloud_run_v2_job_iam_member" "scheduler_invoker" {
  project  = google_cloud_run_v2_job.app_job.project
  location = google_cloud_run_v2_job.app_job.location
  name     = google_cloud_run_v2_job.app_job.name
  role     = "roles/run.invoker"
  member   = "serviceAccount:${google_service_account.scheduler_sa.email}"
}
