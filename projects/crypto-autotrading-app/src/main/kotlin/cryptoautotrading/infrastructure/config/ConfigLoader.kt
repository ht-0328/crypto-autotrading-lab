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

    /**
     * 環境変数やデフォルトのパスから、読み込むべき設定ファイルのパスを解決する。
     *
     * @param configPathEnv 環境変数(APP_CONFIG_PATH)で指定されたパス
     * @return 最終的に使用する設定ファイルのパス
     */
    internal fun resolveConfigPath(configPathEnv: String?): String {
        if (!configPathEnv.isNullOrBlank()) {
            return configPathEnv
        }

        if (File(DEFAULT_CONFIG_PATH).exists()) {
            return DEFAULT_CONFIG_PATH
        }

        return FALLBACK_CONFIG_PATH
    }

    /**
     * デフォルトの設定値を生成する。
     * 設定ファイルが見つからない場合のフォールバックとして使用される。
     *
     * @return デフォルト値が設定されたAppConfig
     */
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
                sharpChangeThreshold = 0.01,
                cooldownLength = 12,
                atrLength = 14,
                atrProfitMultiplier = 2.0,
                atrLossMultiplier = 2.0,
                realTradeEnabled = false,
                dryRun = true,
                maxOrderJpy = 1000,
                maxDailyOrderJpy = 1000,
                maxPositionJpy = 10000,
                gmoApiKeySecretName = "",
                gmoApiSecretSecretName = "",
                orderSymbol = "BTC_JPY",
                orderExecutionType = "LIMIT",
                orderTimeInForce = "FAS",
                stopOnOrderError = true,
                stopOnUnconfirmedOrder = true
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

    /**
     * ベースとなる設定を環境変数の値で上書きする。
     * 環境変数が設定されていない場合は、ベースの設定値をそのまま使用する。
     *
     * @param base ベースとなる設定
     * @return 環境変数で上書きされた最終的なAppConfig
     */
    private fun overrideWithEnvVars(base: AppConfig): AppConfig {
        val envInterval = System.getenv("APP_INTERVAL")
        val envStrategyName = System.getenv("APP_TRADING_STRATEGY_NAME")
        val envSymbol = System.getenv("TRADING_SYMBOL")
        val envInitialCapital = System.getenv("TRADING_INITIAL_CAPITAL")
        val envTradeAmount = System.getenv("TRADING_TRADE_AMOUNT")
        val envBuyThreshold = System.getenv("TRADING_BUY_THRESHOLD")
        val envSellThreshold = System.getenv("TRADING_SELL_THRESHOLD")
        val envVolatilityThreshold = System.getenv("TRADING_VOLATILITY_THRESHOLD")
        val envSharpChangeThreshold = System.getenv("TRADING_SHARP_CHANGE_THRESHOLD")
        val envCooldownLength = System.getenv("TRADING_COOLDOWN_LENGTH")
        val envAtrLength = System.getenv("TRADING_ATR_LENGTH")
        val envAtrProfitMultiplier = System.getenv("TRADING_ATR_PROFIT_MULTIPLIER")
        val envAtrLossMultiplier = System.getenv("TRADING_ATR_LOSS_MULTIPLIER")
        val envRealTradeEnabled = System.getenv("TRADING_REAL_TRADE_ENABLED")
        val envDryRun = System.getenv("TRADING_DRY_RUN")
        val envMaxOrderJpy = System.getenv("TRADING_MAX_ORDER_JPY")
        val envMaxDailyOrderJpy = System.getenv("TRADING_MAX_DAILY_ORDER_JPY")
        val envMaxPositionJpy = System.getenv("TRADING_MAX_POSITION_JPY")
        val envGmoApiKeySecretName = System.getenv("TRADING_GMO_API_KEY_SECRET_NAME")
        val envGmoApiSecretSecretName = System.getenv("TRADING_GMO_API_SECRET_SECRET_NAME")
        val envOrderSymbol = System.getenv("TRADING_ORDER_SYMBOL")
        val envOrderExecutionType = System.getenv("TRADING_ORDER_EXECUTION_TYPE")
        val envOrderTimeInForce = System.getenv("TRADING_ORDER_TIME_IN_FORCE")
        val envStopOnOrderError = System.getenv("TRADING_STOP_ON_ORDER_ERROR")
        val envStopOnUnconfirmedOrder = System.getenv("TRADING_STOP_ON_UNCONFIRMED_ORDER")
        val envRetryCount = System.getenv("API_RETRY_COUNT")
        val envBaseUrl = System.getenv("API_BASE_URL")
        val envOutputPath = System.getenv("OUTPUT_PATH")
        val envStatePath = System.getenv("STATE_PATH")

        return AppConfig(
            app = AppSettings(
                interval = envInterval ?: base.app.interval
            ),
            trading = TradingConfig(
                strategyName = envStrategyName ?: base.trading.strategyName,
                symbol = envSymbol ?: base.trading.symbol,
                initialCapital = envInitialCapital?.toIntOrNull() ?: base.trading.initialCapital,
                tradeAmount = envTradeAmount?.toIntOrNull() ?: base.trading.tradeAmount,
                buyThreshold = envBuyThreshold?.toDoubleOrNull() ?: base.trading.buyThreshold,
                sellThreshold = envSellThreshold?.toDoubleOrNull() ?: base.trading.sellThreshold,
                volatilityThreshold = envVolatilityThreshold?.toDoubleOrNull() ?: base.trading.volatilityThreshold,
                sharpChangeThreshold = envSharpChangeThreshold?.toDoubleOrNull() ?: base.trading.sharpChangeThreshold,
                cooldownLength = envCooldownLength?.toIntOrNull() ?: base.trading.cooldownLength,
                atrLength = envAtrLength?.toIntOrNull() ?: base.trading.atrLength,
                atrProfitMultiplier = envAtrProfitMultiplier?.toDoubleOrNull() ?: base.trading.atrProfitMultiplier,
                atrLossMultiplier = envAtrLossMultiplier?.toDoubleOrNull() ?: base.trading.atrLossMultiplier,
                realTradeEnabled = envRealTradeEnabled?.toBooleanStrictOrNull() ?: base.trading.realTradeEnabled,
                dryRun = envDryRun?.toBooleanStrictOrNull() ?: base.trading.dryRun,
                maxOrderJpy = envMaxOrderJpy?.toIntOrNull() ?: base.trading.maxOrderJpy,
                maxDailyOrderJpy = envMaxDailyOrderJpy?.toIntOrNull() ?: base.trading.maxDailyOrderJpy,
                maxPositionJpy = envMaxPositionJpy?.toIntOrNull() ?: base.trading.maxPositionJpy,
                gmoApiKeySecretName = envGmoApiKeySecretName ?: base.trading.gmoApiKeySecretName,
                gmoApiSecretSecretName = envGmoApiSecretSecretName ?: base.trading.gmoApiSecretSecretName,
                orderSymbol = envOrderSymbol ?: base.trading.orderSymbol,
                orderExecutionType = envOrderExecutionType ?: base.trading.orderExecutionType,
                orderTimeInForce = envOrderTimeInForce ?: base.trading.orderTimeInForce,
                stopOnOrderError = envStopOnOrderError?.toBooleanStrictOrNull() ?: base.trading.stopOnOrderError,
                stopOnUnconfirmedOrder = envStopOnUnconfirmedOrder?.toBooleanStrictOrNull() ?: base.trading.stopOnUnconfirmedOrder
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
