package cryptoautotrading.infrastructure.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import cryptoautotrading.domain.model.ApiConfig
import cryptoautotrading.domain.model.AppConfig
import cryptoautotrading.domain.model.AppSettings
import cryptoautotrading.domain.model.OutputConfig
import cryptoautotrading.domain.model.TradingConfig
import cryptoautotrading.domain.model.realtrading.RealTradingConfig
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
                interval = "5min",
                phase = 1
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
                atrLossMultiplier = 2.0
            ),
            api = ApiConfig(
                retryCount = 3,
                publicBaseUrl = "https://api.coin.z.com/public",
                privateBaseUrl = "https://api.coin.z.com/private"
            ),
            output = OutputConfig(
                outputPath = "trades.csv",
                statePath = "state.json"
            )
        )
    }

    /**
     * 開発フェーズを解決する。
     *
     * フェーズは実注文を許可するかどうかを決める安全上の設定のため、
     * 環境変数が指定されているのに数値として解釈できない場合は、
     * 黙って既定値に戻さず起動時に失敗させる。
     *
     * @param envPhase 環境変数(APP_PHASE)の値
     * @param basePhase 設定ファイル側のフェーズ
     * @return 採用するフェーズ
     */
    internal fun resolvePhase(envPhase: String?, basePhase: Int): Int {
        if (envPhase.isNullOrBlank()) {
            return basePhase
        }
        return envPhase.toIntOrNull()
            ?: error("環境変数 APP_PHASE の値を数値として解釈できません")
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
        val envPhase = System.getenv("APP_PHASE")
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
        val envRetryCount = System.getenv("API_RETRY_COUNT")
        val envPublicBaseUrl = System.getenv("API_PUBLIC_BASE_URL")
        val envPrivateBaseUrl = System.getenv("API_PRIVATE_BASE_URL")
        val envOutputPath = System.getenv("OUTPUT_PATH")
        val envStatePath = System.getenv("STATE_PATH")

        val envRealTradingDryRun = System.getenv("REAL_TRADING_DRY_RUN")
        val envRealTradingEnabled = System.getenv("REAL_TRADING_ENABLED")
        val envStopOnUnconfirmedOrder = System.getenv("REAL_TRADING_STOP_ON_UNCONFIRMED_ORDER")
        val envMaxOrderJpy = System.getenv("REAL_TRADING_MAX_ORDER_JPY")
        val envMaxDailyOrderJpy = System.getenv("REAL_TRADING_MAX_DAILY_ORDER_JPY")
        val envMaxPositionJpy = System.getenv("REAL_TRADING_MAX_POSITION_JPY")

        val finalPublicBaseUrl = envPublicBaseUrl ?: base.api.publicBaseUrl
        val finalPrivateBaseUrl = envPrivateBaseUrl ?: base.api.privateBaseUrl

        return AppConfig(
            app = AppSettings(
                interval = envInterval ?: base.app.interval,
                phase = resolvePhase(envPhase, base.app.phase)
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
                orderSizingMode = base.trading.orderSizingMode
            ),
            api = ApiConfig(
                retryCount = envRetryCount?.toIntOrNull() ?: base.api.retryCount,
                publicBaseUrl = finalPublicBaseUrl,
                privateBaseUrl = finalPrivateBaseUrl
            ),
            output = OutputConfig(
                outputPath = envOutputPath ?: base.output.outputPath,
                statePath = envStatePath ?: base.output.statePath
            ),
            realTrading = RealTradingConfig(
                dryRun = envRealTradingDryRun?.toBooleanStrictOrNull() ?: base.realTrading.dryRun,
                realTradeEnabled = envRealTradingEnabled?.toBooleanStrictOrNull() ?: base.realTrading.realTradeEnabled,
                stopOnUnconfirmedOrder = envStopOnUnconfirmedOrder?.toBooleanStrictOrNull() ?: base.realTrading.stopOnUnconfirmedOrder,
                maxOrderJpy = envMaxOrderJpy?.toIntOrNull() ?: base.realTrading.maxOrderJpy,
                maxDailyOrderJpy = envMaxDailyOrderJpy?.toIntOrNull() ?: base.realTrading.maxDailyOrderJpy,
                maxPositionJpy = envMaxPositionJpy?.toIntOrNull() ?: base.realTrading.maxPositionJpy
            )
        )
    }
}
