package cryptoautotrading.domain.model

import com.fasterxml.jackson.annotation.JsonProperty

data class AppConfig(
    val app: AppSettings,
    val trading: TradingConfig,
    val api: ApiConfig,
    val output: OutputConfig
)

data class AppSettings(
    val interval: String
)

data class TradingConfig(
    val symbol: String,
    @JsonProperty("initial_capital")
    val initialCapital: Int,
    @JsonProperty("trade_amount")
    val tradeAmount: Int,
    @JsonProperty("buy_threshold")
    val buyThreshold: Double,
    @JsonProperty("sell_threshold")
    val sellThreshold: Double
)

data class ApiConfig(
    @JsonProperty("retry_count")
    val retryCount: Int
)

data class OutputConfig(
    @JsonProperty("output_path")
    val outputPath: String,
    @JsonProperty("state_path")
    val statePath: String
)
