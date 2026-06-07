resource "google_cloud_run_v2_job" "app_job" {
  name     = var.cloud_run_job_name
  location = var.region

  template {
    template {
      max_retries = 0

      service_account = google_service_account.runtime_sa.email

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

        # Real trading configs, can be parameterized further if needed.
        # Default to safe values for simulation phase.
        env {
          name  = "REAL_TRADING_DRY_RUN"
          value = "true"
        }
        env {
          name  = "REAL_TRADING_ENABLED"
          value = "false"
        }

        # Secrets via Secret Manager reference
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

# Grant Cloud Scheduler SA permission to invoke this specific Cloud Run Job
resource "google_cloud_run_v2_job_iam_member" "scheduler_invoker" {
  project  = google_cloud_run_v2_job.app_job.project
  location = google_cloud_run_v2_job.app_job.location
  name     = google_cloud_run_v2_job.app_job.name
  role     = "roles/run.invoker"
  member   = "serviceAccount:${google_service_account.scheduler_sa.email}"
}
