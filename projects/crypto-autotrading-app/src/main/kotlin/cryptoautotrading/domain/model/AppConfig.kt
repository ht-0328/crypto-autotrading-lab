package cryptoautotrading.domain.model

import com.fasterxml.jackson.annotation.JsonProperty
import cryptoautotrading.domain.model.realtrading.RealTradingConfig

/**
 * アプリケーション全体の設定を保持するルートクラス
 *
 * @property app アプリケーションの基本設定
 * @property trading 取引関連の設定
 * @property api 外部API関連の設定
 * @property output データ出力関連の設定
 * @property realTrading リアル取引関連の設定。未設定時は安全側のデフォルト値を使用する。
 */
data class AppConfig(
    val app: AppSettings,
    val trading: TradingConfig,
    val api: ApiConfig,
    val output: OutputConfig,
    @JsonProperty("real_trading")
    val realTrading: RealTradingConfig = RealTradingConfig()
)
