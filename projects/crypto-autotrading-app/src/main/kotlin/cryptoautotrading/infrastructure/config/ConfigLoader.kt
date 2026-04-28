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

/**
 * 設定ファイルを読み込むためのオブジェクト
 */
object ConfigLoader {

    private val logger = KotlinLogging.logger {}
    private val mapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule.Builder().build())
    private const val DEFAULT_CONFIG_PATH = "config/application-gmo.yaml"
    private const val FALLBACK_CONFIG_PATH = "../../config/application-gmo.yaml"

    /**
     * アプリケーション設定を読み込む
     *
     * @return 読み込んだAppConfig
     */
    fun load(): AppConfig {
        logger.info { "ConfigLoader: 設定の読み込みを開始します" }

        val configPath = resolveConfigPath(System.getenv("APP_CONFIG_PATH"))
        val file = File(configPath)

        logger.debug { "ConfigLoader: 設定ファイルのパス = ${file.absolutePath}" }

        val baseConfig = if (file.exists()) {
            logger.info { "ConfigLoader: 設定ファイルが見つかりました (${file.absolutePath})" }
            mapper.readValue(file, AppConfig::class.java)
        } else {
            logger.warn { "ConfigLoader: 設定ファイルが見つかりません。デフォルト値を使用します。(${file.absolutePath})" }
            createDefaultConfig()
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

    private fun createDefaultConfig(): AppConfig {
        return AppConfig(
            app = AppSettings(
                interval = "5min"
            ),
            trading = TradingConfig(
                symbol = "BTC",
                initialCapital = 10000,
                tradeAmount = 1000,
                buyThreshold = 0.005,
                sellThreshold = 0.005,
                volatilityThreshold = 0.003,
                sharpChangeThreshold = 0.01
            ),
            api = ApiConfig(
                retryCount = 3,
                baseUrl = "https://api.coin.z.com"
            ),
            output = OutputConfig(
                outputPath = "trades.csv",
                statePath = "state.json"
            )
        )
    }

    private fun overrideWithEnvVars(base: AppConfig): AppConfig {
        val envInterval = System.getenv("APP_INTERVAL")
        val envSymbol = System.getenv("TRADING_SYMBOL")
        val envInitialCapital = System.getenv("TRADING_INITIAL_CAPITAL")
        val envTradeAmount = System.getenv("TRADING_TRADE_AMOUNT")
        val envBuyThreshold = System.getenv("TRADING_BUY_THRESHOLD")
        val envSellThreshold = System.getenv("TRADING_SELL_THRESHOLD")
        val envVolatilityThreshold = System.getenv("TRADING_VOLATILITY_THRESHOLD")
        val envSharpChangeThreshold = System.getenv("TRADING_SHARP_CHANGE_THRESHOLD")
        val envRetryCount = System.getenv("API_RETRY_COUNT")
        val envBaseUrl = System.getenv("API_BASE_URL")
        val envOutputPath = System.getenv("OUTPUT_PATH")
        val envStatePath = System.getenv("STATE_PATH")

        return AppConfig(
            app = AppSettings(
                interval = envInterval ?: base.app.interval
            ),
            trading = TradingConfig(
                symbol = envSymbol ?: base.trading.symbol,
                initialCapital = envInitialCapital?.toIntOrNull() ?: base.trading.initialCapital,
                tradeAmount = envTradeAmount?.toIntOrNull() ?: base.trading.tradeAmount,
                buyThreshold = envBuyThreshold?.toDoubleOrNull() ?: base.trading.buyThreshold,
                sellThreshold = envSellThreshold?.toDoubleOrNull() ?: base.trading.sellThreshold,
                volatilityThreshold = envVolatilityThreshold?.toDoubleOrNull() ?: base.trading.volatilityThreshold,
                sharpChangeThreshold = envSharpChangeThreshold?.toDoubleOrNull() ?: base.trading.sharpChangeThreshold
            ),
            api = ApiConfig(
                retryCount = envRetryCount?.toIntOrNull() ?: base.api.retryCount,
                baseUrl = envBaseUrl ?: base.api.baseUrl
            ),
            output = OutputConfig(
                outputPath = envOutputPath ?: base.output.outputPath,
                statePath = envStatePath ?: base.output.statePath
            )
        )
    }
}
