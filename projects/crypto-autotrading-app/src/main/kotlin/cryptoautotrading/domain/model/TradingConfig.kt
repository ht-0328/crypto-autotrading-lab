package cryptoautotrading.domain.model

import com.fasterxml.jackson.annotation.JsonProperty

data class TradingConfig(
    val strategyName: String = "SafeReboundStrategy",
    val symbol: String,
    val initialCapital: Int,
    val tradeAmount: Int,
    val buyThreshold: Double,
    val sellThreshold: Double,
    val volatilityThreshold: Double,
    val sharpChangeThreshold: Double,
    @JsonProperty("cooldownLength") val cooldownLength: Int = 12,
    @JsonProperty("atrLength") val atrLength: Int = 14,
    @JsonProperty("atrProfitMultiplier") val atrProfitMultiplier: Double = 2.0,
    @JsonProperty("atrLossMultiplier") val atrLossMultiplier: Double = 2.0,

    val realTradeEnabled: Boolean = false,
    val dryRun: Boolean = true,
    val maxOrderJpy: Int = 1000,
    val maxDailyOrderJpy: Int = 10000,
    val maxPositionJpy: Int = 10000,
    val gmoApiKeySecretName: String = "gmo-api-key",
    val gmoApiSecretSecretName: String = "gmo-api-secret",
    val orderSymbol: String = "BTC_JPY",
    val orderExecutionType: String = "LIMIT",
    val orderTimeInForce: String = "FAS",
    val stopOnOrderError: Boolean = true,
    val stopOnUnconfirmedOrder: Boolean = true
)
