variable "project_id" {
  description = "GCP プロジェクトID"
  type        = string
}

variable "region" {
  description = "GCP リージョン"
  type        = string
  default     = "asia-northeast1"
}

variable "artifact_repository_name" {
  description = "Artifact Registry リポジトリ名"
  type        = string
}

variable "gcs_bucket_name" {
  description = "アプリケーションが利用する GCS Bucket 名"
  type        = string
}

variable "build_service_account_name" {
  description = "Cloud Build 実行用 Service Account 名"
  type        = string
}

variable "runtime_service_account_name" {
  description = "Cloud Run Job 実行用 Service Account 名"
  type        = string
}

variable "scheduler_service_account_name" {
  description = "Cloud Scheduler 実行用 Service Account 名"
  type        = string
}

variable "deploy_service_account_email" {
  description = "GitHub Actions からデプロイを行うための Service Account のメールアドレス"
  type        = string
}

variable "secret_names" {
  description = "Secret Manager に作成する Secret の名前リスト"
  type        = list(string)
  default     = ["gmo-api-key", "gmo-api-secret"]
}

variable "cloud_run_job_name" {
  description = "Cloud Run Job 名"
  type        = string
}

variable "image_uri" {
  description = "Cloud Run Job で実行する Docker イメージの URI（固定タグ付きを想定）"
  type        = string
}

variable "app_trading_strategy_name" {
  description = "取引戦略名"
  type        = string
  default     = "SafeReboundStrategy"
}

variable "api_public_base_url" {
  description = "Public API のベース URL"
  type        = string
  default     = "https://api.coin.z.com/public"
}

variable "api_private_base_url" {
  description = "Private API のベース URL"
  type        = string
  default     = "https://api.coin.z.com/private"
}

variable "api_retry_count" {
  description = "API リトライ回数"
  type        = string
  default     = "3"
}

variable "app_interval" {
  description = "アプリケーションの実行間隔（秒）"
  type        = string
  default     = "60"
}

variable "output_path" {
  # APP_DATA_DIR からの相対パスで指定する。絶対パスを渡すと APP_DATA_DIR に連結されてしまう
  # （例: /mnt/gcs/data + /mnt/gcs/data/output.json = /mnt/gcs/data/mnt/gcs/data/output.json）
  description = "取引履歴CSVのファイル名（APP_DATA_DIR からの相対パス）"
  type        = string
  default     = "trades.csv"
}

variable "state_path" {
  # APP_DATA_DIR からの相対パスで指定する
  description = "状態ファイルのファイル名（APP_DATA_DIR からの相対パス）"
  type        = string
  default     = "state.json"
}

variable "trading_order_sizing_mode" {
  description = "注文サイズモード（FIXED_AMOUNT または ALL_IN）"
  type        = string
  default     = "FIXED_AMOUNT"
}

variable "trading_buy_threshold" {
  description = "取引の買い判定しきい値"
  type        = string
}

variable "trading_sell_threshold" {
  description = "取引の売り判定しきい値"
  type        = string
}

variable "trading_initial_capital" {
  description = "取引の初期資金"
  type        = string
}

variable "trading_trade_amount" {
  description = "1回あたりの取引金額"
  type        = string
}

variable "trading_symbol" {
  description = "取引対象のシンボル"
  type        = string
  default     = "BTC_JPY"
}

variable "trading_volatility_threshold" {
  description = "取引のボラティリティ判定値"
  type        = string
}

variable "trading_sharp_change_threshold" {
  description = "取引の急変判定値"
  type        = string
}

variable "trading_cooldown_length" {
  description = "取引のクールダウン期間"
  type        = string
}

variable "trading_atr_length" {
  description = "取引の ATR 算出期間"
  type        = string
}

variable "trading_atr_profit_multiplier" {
  description = "取引の ATR 利益乗数"
  type        = string
}

variable "trading_atr_loss_multiplier" {
  description = "取引の ATR 損失乗数"
  type        = string
}

variable "scheduler_job_name" {
  description = "Cloud Scheduler ジョブ名"
  type        = string
}

variable "scheduler_cron" {
  description = "Cloud Scheduler の定期実行 cron 式"
  type        = string
  default     = "0 9 * * *"
}

variable "scheduler_time_zone" {
  description = "Cloud Scheduler のタイムゾーン"
  type        = string
  default     = "Asia/Tokyo"
}

variable "real_trading_stop_on_unconfirmed_order" {
  description = "未確認注文がある場合にリアル取引を停止する設定"
  type        = string
  default     = "true"
}

variable "real_trading_max_order_jpy" {
  description = "リアル取引の1回あたり最大注文金額（円）"
  type        = string
  default     = "0"
}

variable "real_trading_max_daily_order_jpy" {
  description = "リアル取引の1日あたり最大注文金額（円）"
  type        = string
  default     = "0"
}

variable "real_trading_max_position_jpy" {
  description = "リアル取引の最大保有金額（円）"
  type        = string
  default     = "0"
}
