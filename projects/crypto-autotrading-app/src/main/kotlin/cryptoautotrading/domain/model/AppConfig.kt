package cryptoautotrading.domain.model

/**
 * アプリケーション全体の設定を保持するルートクラス
 *
 * @property app アプリケーションの基本設定
 * @property trading 取引関連の設定
 * @property api 外部API関連の設定
 * @property output データ出力関連の設定
 */
data class AppConfig(
    val app: AppSettings,
    val trading: TradingConfig,
    val api: ApiConfig,
    val output: OutputConfig
)
