package cryptoautotrading.domain.model

import com.fasterxml.jackson.annotation.JsonProperty

data class TradingConfig(
    @JsonProperty("strategy_name")
    val strategyName: String = "SafeReboundStrategy",
    val symbol: String,
    @JsonProperty("initial_capital")
    val initialCapital: Int,
    @JsonProperty("trade_amount")
    val tradeAmount: Int,
    @JsonProperty("buy_threshold")
    val buyThreshold: Double,
    @JsonProperty("sell_threshold")
    val sellThreshold: Double,
    @JsonProperty("volatility_threshold")
    val volatilityThreshold: Double,
    @JsonProperty("sharp_change_threshold")
    val sharpChangeThreshold: Double,
    @JsonProperty("cooldown_length")
    val cooldownLength: Int = 12,
    @JsonProperty("atr_length")
    val atrLength: Int = 14,
    @JsonProperty("atr_profit_multiplier")
    val atrProfitMultiplier: Double = 2.0,
    @JsonProperty("atr_loss_multiplier")
    val atrLossMultiplier: Double = 2.0
)
