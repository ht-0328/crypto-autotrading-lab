variable "project_id" {
  description = "GCP Project ID"
  type        = string
}

variable "region" {
  description = "GCP Region"
  type        = string
  default     = "asia-northeast1"
}

variable "artifact_repository_name" {
  description = "Artifact Registry name"
  type        = string
}

variable "gcs_bucket_name" {
  description = "App-use GCS Bucket name"
  type        = string
}

variable "build_service_account_name" {
  description = "Cloud Build Service Account name"
  type        = string
}

variable "runtime_service_account_name" {
  description = "Cloud Run Runtime Service Account name"
  type        = string
}

variable "scheduler_service_account_name" {
  description = "Cloud Scheduler Service Account name"
  type        = string
}

variable "deploy_service_account_email" {
  description = "GitHub Actions Deploy Service Account email"
  type        = string
}

variable "secret_names" {
  description = "List of Secret Manager secret names"
  type        = list(string)
  default     = ["gmo-api-key", "gmo-api-secret"]
}

variable "cloud_run_job_name" {
  description = "Cloud Run Job name"
  type        = string
}

variable "image_uri" {
  description = "Docker image URI for Cloud Run Job (e.g. including fixed SHA tag)"
  type        = string
}

variable "app_trading_strategy_name" {
  description = "Trading strategy name"
  type        = string
  default     = "SafeReboundStrategy"
}

variable "api_public_base_url" {
  description = "Public API base URL"
  type        = string
  default     = "https://api.coin.z.com/public"
}

variable "api_private_base_url" {
  description = "Private API base URL"
  type        = string
  default     = "https://api.coin.z.com/private"
}

variable "api_retry_count" {
  description = "API retry count"
  type        = string
  default     = "3"
}

variable "app_interval" {
  description = "App execution interval in seconds"
  type        = string
  default     = "60"
}

variable "output_path" {
  description = "Result output path"
  type        = string
  default     = "/mnt/gcs/data/output.json"
}

variable "state_path" {
  description = "State file save path"
  type        = string
  default     = "/mnt/gcs/data/state.json"
}

variable "trading_buy_threshold" {
  description = "Trading buy threshold"
  type        = string
}

variable "trading_sell_threshold" {
  description = "Trading sell threshold"
  type        = string
}

variable "trading_initial_capital" {
  description = "Trading initial capital"
  type        = string
}

variable "trading_trade_amount" {
  description = "Trading trade amount per transaction"
  type        = string
}

variable "trading_symbol" {
  description = "Trading symbol"
  type        = string
  default     = "BTC_JPY"
}

variable "trading_volatility_threshold" {
  description = "Trading volatility threshold"
  type        = string
}

variable "trading_sharp_change_threshold" {
  description = "Trading sharp change threshold"
  type        = string
}

variable "trading_cooldown_length" {
  description = "Trading cooldown length"
  type        = string
}

variable "trading_atr_length" {
  description = "Trading ATR length"
  type        = string
}

variable "trading_atr_profit_multiplier" {
  description = "Trading ATR profit multiplier"
  type        = string
}

variable "trading_atr_loss_multiplier" {
  description = "Trading ATR loss multiplier"
  type        = string
}

variable "scheduler_job_name" {
  description = "Cloud Scheduler job name"
  type        = string
}

variable "scheduler_cron" {
  description = "Cron expression for Cloud Scheduler"
  type        = string
  default     = "0 9 * * *"
}

variable "scheduler_time_zone" {
  description = "Time zone for Cloud Scheduler"
  type        = string
  default     = "Asia/Tokyo"
}

variable "real_trading_stop_on_unconfirmed_order" {
  description = "Real trading stop on unconfirmed order"
  type        = string
  default     = "true"
}

variable "real_trading_max_order_jpy" {
  description = "Real trading max order jpy"
  type        = string
  default     = "0"
}

variable "real_trading_max_daily_order_jpy" {
  description = "Real trading max daily order jpy"
  type        = string
  default     = "0"
}

variable "real_trading_max_position_jpy" {
  description = "Real trading max position jpy"
  type        = string
  default     = "0"
}
