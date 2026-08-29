package cryptoautotrading.infrastructure.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import cryptoautotrading.domain.model.ApiConfig
import cryptoautotrading.domain.model.AppConfig
import cryptoautotrading.domain.model.AppSettings
import cryptoautotrading.domain.model.OrderSizingMode
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
     * 環境変数を文字列として取得する。
     *
     * 空文字・空白のみの場合は「未指定」として扱い null を返す。
     * GitHub Actions は未登録の Variable を空文字として渡すため、
     * 空文字を値として採用すると URL などが空のまま起動してしまう。
     *
     * @param name 環境変数名
     * @return 指定されていれば値、未指定なら null
     */
    private fun envString(name: String): String? {
        return System.getenv(name)?.takeIf { it.isNotBlank() }
    }

    /**
     * 環境変数の値を Int として解釈する。
     *
     * 値が指定されているのに数値として解釈できない場合は起動時に失敗させる。
     * 黙ってベース値に戻すと、設定ミスに気付けないまま運用してしまう。
     * 例外メッセージには値を含めない（設定値が秘密情報である可能性があるため）。
     *
     * @param name 環境変数名（エラーメッセージ用）
     * @param raw 環境変数の生の値
     * @return 解釈した値。未指定なら null
     */
    internal fun parseIntEnv(name: String, raw: String?): Int? {
        val value = raw?.takeIf { it.isNotBlank() } ?: return null
        return value.toIntOrNull() ?: error("環境変数 $name の値を数値として解釈できません")
    }

    /**
     * 環境変数の値を Double として解釈する。
     *
     * @param name 環境変数名（エラーメッセージ用）
     * @param raw 環境変数の生の値
     * @return 解釈した値。未指定なら null
     */
    internal fun parseDoubleEnv(name: String, raw: String?): Double? {
        val value = raw?.takeIf { it.isNotBlank() } ?: return null
        return value.toDoubleOrNull() ?: error("環境変数 $name の値を数値として解釈できません")
    }

    /**
     * 環境変数の値を Boolean として解釈する。
     *
     * @param name 環境変数名（エラーメッセージ用）
     * @param raw 環境変数の生の値
     * @return 解釈した値。未指定なら null
     */
    internal fun parseBooleanEnv(name: String, raw: String?): Boolean? {
        val value = raw?.takeIf { it.isNotBlank() } ?: return null
        return value.toBooleanStrictOrNull()
            ?: error("環境変数 $name の値を真偽値として解釈できません。true または false を指定してください")
    }

    /**
     * 環境変数の値を注文サイズモードとして解釈する。
     *
     * @param name 環境変数名（エラーメッセージ用）
     * @param raw 環境変数の生の値
     * @return 解釈した値。未指定なら null
     */
    internal fun parseOrderSizingModeEnv(name: String, raw: String?): OrderSizingMode? {
        val value = raw?.takeIf { it.isNotBlank() } ?: return null
        return OrderSizingMode.entries.firstOrNull { it.name == value }
            ?: error(
                "環境変数 $name の値が不正です。" +
                    "指定できるのは ${OrderSizingMode.entries.joinToString(", ") { it.name }} です"
            )
    }

    /**
     * 環境変数を Int として取得する。
     *
     * @param name 環境変数名
     * @param base 未指定時に使用するベースの値
     * @return 採用する値
     */
    private fun envInt(name: String, base: Int): Int = parseIntEnv(name, System.getenv(name)) ?: base

    /**
     * 環境変数を Int として取得する（未設定を許容する項目用）。
     *
     * @param name 環境変数名
     * @param base 未指定時に使用するベースの値
     * @return 採用する値。どちらも未指定なら null
     */
    private fun envIntOrNull(name: String, base: Int?): Int? = parseIntEnv(name, System.getenv(name)) ?: base

    /**
     * 環境変数を Double として取得する。
     *
     * @param name 環境変数名
     * @param base 未指定時に使用するベースの値
     * @return 採用する値
     */
    private fun envDouble(name: String, base: Double): Double = parseDoubleEnv(name, System.getenv(name)) ?: base

    /**
     * 環境変数を Boolean として取得する。
     *
     * @param name 環境変数名
     * @param base 未指定時に使用するベースの値
     * @return 採用する値
     */
    private fun envBoolean(name: String, base: Boolean): Boolean = parseBooleanEnv(name, System.getenv(name)) ?: base

    /**
     * 環境変数を注文サイズモードとして取得する。
     *
     * @param name 環境変数名
     * @param base 未指定時に使用するベースの値
     * @return 採用する値
     */
    private fun envOrderSizingMode(name: String, base: OrderSizingMode): OrderSizingMode =
        parseOrderSizingModeEnv(name, System.getenv(name)) ?: base

    /**
     * ベースとなる設定を環境変数の値で上書きする。
     * 環境変数が設定されていない場合は、ベースの設定値をそのまま使用する。
     *
     * @param base ベースとなる設定
     * @return 環境変数で上書きされた最終的なAppConfig
     */
    private fun overrideWithEnvVars(base: AppConfig): AppConfig {
        return AppConfig(
            app = AppSettings(
                interval = envString("APP_INTERVAL") ?: base.app.interval,
                phase = resolvePhase(System.getenv("APP_PHASE"), base.app.phase)
            ),
            trading = TradingConfig(
                strategyName = envString("APP_TRADING_STRATEGY_NAME") ?: base.trading.strategyName,
                symbol = envString("TRADING_SYMBOL") ?: base.trading.symbol,
                initialCapital = envInt("TRADING_INITIAL_CAPITAL", base.trading.initialCapital),
                tradeAmount = envInt("TRADING_TRADE_AMOUNT", base.trading.tradeAmount),
                buyThreshold = envDouble("TRADING_BUY_THRESHOLD", base.trading.buyThreshold),
                sellThreshold = envDouble("TRADING_SELL_THRESHOLD", base.trading.sellThreshold),
                volatilityThreshold = envDouble("TRADING_VOLATILITY_THRESHOLD", base.trading.volatilityThreshold),
                sharpChangeThreshold = envDouble("TRADING_SHARP_CHANGE_THRESHOLD", base.trading.sharpChangeThreshold),
                cooldownLength = envInt("TRADING_COOLDOWN_LENGTH", base.trading.cooldownLength),
                atrLength = envInt("TRADING_ATR_LENGTH", base.trading.atrLength),
                atrProfitMultiplier = envDouble("TRADING_ATR_PROFIT_MULTIPLIER", base.trading.atrProfitMultiplier),
                atrLossMultiplier = envDouble("TRADING_ATR_LOSS_MULTIPLIER", base.trading.atrLossMultiplier),
                orderSizingMode = envOrderSizingMode("TRADING_ORDER_SIZING_MODE", base.trading.orderSizingMode)
            ),
            api = ApiConfig(
                retryCount = envInt("API_RETRY_COUNT", base.api.retryCount),
                publicBaseUrl = envString("API_PUBLIC_BASE_URL") ?: base.api.publicBaseUrl,
                privateBaseUrl = envString("API_PRIVATE_BASE_URL") ?: base.api.privateBaseUrl
            ),
            output = OutputConfig(
                outputPath = envString("OUTPUT_PATH") ?: base.output.outputPath,
                statePath = envString("STATE_PATH") ?: base.output.statePath
            ),
            realTrading = RealTradingConfig(
                dryRun = envBoolean("REAL_TRADING_DRY_RUN", base.realTrading.dryRun),
                realTradeEnabled = envBoolean("REAL_TRADING_ENABLED", base.realTrading.realTradeEnabled),
                stopOnUnconfirmedOrder = resolveStopOnUnconfirmedOrder(base.realTrading.stopOnUnconfirmedOrder),
                maxOrderJpy = envIntOrNull("REAL_TRADING_MAX_ORDER_JPY", base.realTrading.maxOrderJpy),
                maxDailyOrderJpy = envIntOrNull("REAL_TRADING_MAX_DAILY_ORDER_JPY", base.realTrading.maxDailyOrderJpy),
                maxPositionJpy = envIntOrNull("REAL_TRADING_MAX_POSITION_JPY", base.realTrading.maxPositionJpy)
            )
        )
    }

    /**
     * 未確認注文がある場合の停止設定を解決する。
     *
     * この設定は現在の実装では効果を持たない。`RealTradingSafetyChecker` は値に関わらず
     * 未確認注文があれば必ず注文を見送る。安全側に倒す方針のため、この動作は変えない。
     * `false` が指定された場合は、設定が効いていないことに気付けるよう警告を出す。
     *
     * @param base 設定ファイル側の値
     * @return 採用する値
     */
    private fun resolveStopOnUnconfirmedOrder(base: Boolean): Boolean {
        val value = envBoolean("REAL_TRADING_STOP_ON_UNCONFIRMED_ORDER", base)
        if (!value) {
            logger.warn {
                "stop_on_unconfirmed_order に false が指定されていますが、この設定は現在有効になりません。" +
                    "未確認注文がある場合は、値に関わらず新規注文を見送ります。"
            }
        }
        return value
    }
}
