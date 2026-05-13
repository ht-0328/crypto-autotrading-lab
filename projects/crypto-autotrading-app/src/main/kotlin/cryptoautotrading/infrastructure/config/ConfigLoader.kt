package cryptoautotrading.infrastructure.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import cryptoautotrading.domain.model.ApiConfig
import cryptoautotrading.domain.model.AppConfig
import cryptoautotrading.domain.model.AppSettings
import cryptoautotrading.domain.model.OutputConfig
import cryptoautotrading.domain.model.TradingConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

object ConfigLoader {
    private val logger = KotlinLogging.logger {}
    private val mapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule.Builder().build())
    private const val DEFAULT_CONFIG_PATH = "config/application-gmo.yaml"
    private const val FALLBACK_CONFIG_PATH = "../../config/application-gmo.yaml"

    fun load(): AppConfig {
        logger.info { "ConfigLoader: 設定の読み込みを開始します" }

        val configPath = resolveConfigPath(System.getenv("APP_CONFIG_PATH"))
        val file = File(configPath)

        logger.debug { "ConfigLoader: 設定ファイルのパス = \${file.absolutePath}" }

        val baseConfig = if (file.exists()) {
            logger.info { "ConfigLoader: 設定ファイルが見つかりました (\${file.absolutePath})" }
            mapper.readValue(file, AppConfig::class.java)
        } else {
            logger.error { "ConfigLoader: 必須の設定ファイルが見つかりません。(\${file.absolutePath})" }
            throw IllegalStateException("設定ファイルが見つかりません: \${file.absolutePath}")
        }

        val finalConfig = overrideWithEnvVars(baseConfig)

        logger.info { "ConfigLoader: 設定の読み込みが完了しました" }
        return finalConfig
    }

    internal fun resolveConfigPath(configPathEnv: String?): String {
        if (!configPathEnv.isNullOrBlank()) {
            return configPathEnv
        }
        if (File(DEFAULT_CONFIG_PATH).exists()) {
            return DEFAULT_CONFIG_PATH
        }
        return FALLBACK_CONFIG_PATH
    }

    private fun overrideWithEnvVars(base: AppConfig): AppConfig {
        return AppConfig(
            app = AppSettings(interval = System.getenv("APP_INTERVAL") ?: base.app.interval),
            trading = TradingConfig(
                strategyName = System.getenv("APP_TRADING_STRATEGY_NAME") ?: base.trading.strategyName,
                symbol = System.getenv("TRADING_SYMBOL") ?: base.trading.symbol,
                initialCapital = System.getenv("TRADING_INITIAL_CAPITAL")?.toIntOrNull() ?: base.trading.initialCapital,
                tradeAmount = System.getenv("TRADING_TRADE_AMOUNT")?.toIntOrNull() ?: base.trading.tradeAmount,
                buyThreshold = System.getenv("TRADING_BUY_THRESHOLD")?.toDoubleOrNull() ?: base.trading.buyThreshold,
                sellThreshold = System.getenv("TRADING_SELL_THRESHOLD")?.toDoubleOrNull() ?: base.trading.sellThreshold,
                volatilityThreshold = System.getenv("TRADING_VOLATILITY_THRESHOLD")?.toDoubleOrNull() ?: base.trading.volatilityThreshold,
                sharpChangeThreshold = System.getenv("TRADING_SHARP_CHANGE_THRESHOLD")?.toDoubleOrNull() ?: base.trading.sharpChangeThreshold,
                cooldownLength = System.getenv("TRADING_COOLDOWN_LENGTH")?.toIntOrNull() ?: base.trading.cooldownLength,
                atrLength = System.getenv("TRADING_ATR_LENGTH")?.toIntOrNull() ?: base.trading.atrLength,
                atrProfitMultiplier = System.getenv("TRADING_ATR_PROFIT_MULTIPLIER")?.toDoubleOrNull() ?: base.trading.atrProfitMultiplier,
                atrLossMultiplier = System.getenv("TRADING_ATR_LOSS_MULTIPLIER")?.toDoubleOrNull() ?: base.trading.atrLossMultiplier,

                realTradeEnabled = System.getenv("TRADING_REAL_TRADE_ENABLED")?.toBooleanStrictOrNull() ?: base.trading.realTradeEnabled,
                dryRun = System.getenv("TRADING_DRY_RUN")?.toBooleanStrictOrNull() ?: base.trading.dryRun,
                maxOrderJpy = System.getenv("TRADING_MAX_ORDER_JPY")?.toIntOrNull() ?: base.trading.maxOrderJpy,
                maxDailyOrderJpy = base.trading.maxDailyOrderJpy,
                maxPositionJpy = base.trading.maxPositionJpy,
                gmoApiKeySecretName = base.trading.gmoApiKeySecretName,
                gmoApiSecretSecretName = base.trading.gmoApiSecretSecretName,
                orderSymbol = base.trading.orderSymbol,
                orderExecutionType = base.trading.orderExecutionType,
                orderTimeInForce = base.trading.orderTimeInForce,
                stopOnOrderError = base.trading.stopOnOrderError,
                stopOnUnconfirmedOrder = base.trading.stopOnUnconfirmedOrder
            ),
            api = ApiConfig(
                retryCount = System.getenv("API_RETRY_COUNT")?.toIntOrNull() ?: base.api.retryCount,
                baseUrl = System.getenv("API_BASE_URL") ?: base.api.baseUrl
            ),
            output = OutputConfig(
                outputPath = System.getenv("OUTPUT_PATH") ?: base.output.outputPath,
                statePath = System.getenv("STATE_PATH") ?: base.output.statePath
            )
        )
    }
}
