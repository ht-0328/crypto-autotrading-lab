package cryptoautotrading.presentation

import cryptoautotrading.application.TradingApplication
import cryptoautotrading.domain.model.realtrading.RealTradingConfig
import cryptoautotrading.infrastructure.config.ConfigLoader
import cryptoautotrading.infrastructure.exchange.gmo.GmoPrivateApiClientImpl
import cryptoautotrading.infrastructure.exchange.gmo.GmoPublicApiClient
import cryptoautotrading.infrastructure.exchange.gmo.auth.DummyGmoCredentialProvider
import cryptoautotrading.infrastructure.exchange.gmo.auth.EnvGmoCredentialProvider
import cryptoautotrading.infrastructure.exchange.gmo.auth.GmoSignatureGeneratorImpl
import cryptoautotrading.infrastructure.output.ConsoleOutput
import cryptoautotrading.infrastructure.output.CsvRepository
import cryptoautotrading.infrastructure.output.StateRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.runBlocking
import java.io.File
import java.math.BigDecimal
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

/**
 * 実注文が許可される最小のフェーズ。
 * ロードマップ上、実注文は Phase3（実注文 + 安全制御）のスコープ。
 */
private const val REAL_TRADING_ALLOWED_PHASE = 3

/**
 * アプリケーションのエントリーポイント
 */
fun main() = runBlocking {
    logger.info { "Crypto Auto-Trading Lab 起動処理を開始します" }

    try {
        // 設定を読み込む
        logger.info { "設定ファイルの読み込みを開始します" }
        val config = ConfigLoader.load()
        logger.info { "設定ファイルの読み込みが完了しました" }

        // データディレクトリの初期化
        val dataDirEnv = System.getenv("APP_DATA_DIR")
        val finalDir = if (dataDirEnv.isNullOrBlank()) {
            logger.warn { "APP_DATA_DIR が未設定です。デフォルトの './data' を使用します。" }
            "./data"
        } else {
            dataDirEnv
        }

        val dirFile = File(finalDir)
        if (!dirFile.exists()) {
            dirFile.mkdirs()
        }

        // リポジトリの設定パス解決
        val statePath = Paths.get(finalDir, config.output.statePath).toString()
        val csvPath = Paths.get(finalDir, config.output.outputPath).toString()

        val stateRepository = StateRepository(statePath)
        val csvRepository = CsvRepository(csvPath)
        val resultOutputPort = ConsoleOutput

        // APIのベースURLを設定ファイルから取得する
        val publicBaseUrl = config.api.publicBaseUrl ?: "https://api.coin.z.com/public"
        val privateBaseUrl = config.api.privateBaseUrl ?: "https://api.coin.z.com/private"
        val retryCount = config.api.retryCount
        logger.info { "最終的に採用したAPIベースURL(Public): $publicBaseUrl, APIベースURL(Private): $privateBaseUrl, リトライ回数: $retryCount" }

        // 実注文が有効な場合のみ、Private API 用のクライアントと認証情報を初期化する
        val isRealTradeActive = config.realTrading.realTradeEnabled && !config.realTrading.dryRun

        // Phase3 未満では、設定値にかかわらず実注文経路に入らせない
        if (config.app.phase < REAL_TRADING_ALLOWED_PHASE && isRealTradeActive) {
            logger.error {
                "Phase${config.app.phase} では実注文を実行できません。" +
                    "real_trade_enabled=false または dry_run=true に戻してください。" +
                    "実注文は Phase$REAL_TRADING_ALLOWED_PHASE 以降でのみ許可されます。"
            }
            error("Phase${config.app.phase} で実注文が有効化されています")
        }

        // 注文数量の制約が未設定のまま実注文すると、取引所の刻みに合わない数量を送って
        // 拒否され続ける。設定漏れは起動時に気づけるようにする。
        if (isRealTradeActive) {
            validateOrderSizeSettings(config.realTrading)
            validateOrderPriceSettings(config.realTrading)
        }

        if (isRealTradeActive) {
            val isWireMockPrivateApi = privateBaseUrl.contains("wiremock") ||
                privateBaseUrl.contains("localhost")

            val credentialProvider = if (isWireMockPrivateApi) {
                DummyGmoCredentialProvider()
            } else {
                EnvGmoCredentialProvider()
            }

            HttpClient(CIO).use { httpClient ->
                val privateApiClient = GmoPrivateApiClientImpl(
                    httpClient = httpClient,
                    baseUrl = privateBaseUrl,
                    signatureGenerator = GmoSignatureGeneratorImpl(),
                    credentialProvider = credentialProvider
                )

                GmoPublicApiClient(publicBaseUrl, retryCount).use { apiClient ->
                    val app = TradingApplication(
                        config = config,
                        marketDataClient = apiClient,
                        stateRepository = stateRepository,
                        tradeHistoryRepository = csvRepository,
                        resultOutputPort = resultOutputPort,
                        realTradingExchangeClient = privateApiClient
                    )

                    logger.info { "TradingApplication の実行を開始します(実注文有効)" }
                    app.run()
                    logger.info { "TradingApplication の実行が終了しました" }
                }
            }
        } else {
            GmoPublicApiClient(publicBaseUrl, retryCount).use { apiClient ->
                val app = TradingApplication(
                    config = config,
                    marketDataClient = apiClient,
                    stateRepository = stateRepository,
                    tradeHistoryRepository = csvRepository,
                    resultOutputPort = resultOutputPort,
                    realTradingExchangeClient = null
                )

                logger.info { "TradingApplication の実行を開始します(シミュレーションのみ)" }
                app.run()
                logger.info { "TradingApplication の実行が終了しました" }
            }
        }
    } catch (e: Exception) {
        logger.error(e) { "アプリケーションの起動・実行中に予期せぬエラーが発生しました: ${e.message}" }
        throw e
    } finally {
        logger.info { "Crypto Auto-Trading Lab 起動処理が終了しました" }
    }
}

/**
 * 実注文に必要な注文数量の設定が揃っているかを検証する。
 *
 * 取引所は最小注文数量と数量の刻みを持つ。刻みに合わない数量を送ると注文は拒否されるため、
 * 設定が無いまま実注文を有効にすると、注文のたびに失敗して停止することになる。
 * 設定漏れは実行時ではなく起動時に気づけるようにする。
 *
 * @param realTradingConfig リアル取引設定
 * @throws IllegalStateException 最小注文数量または刻みが未設定、もしくは正の数でない場合
 */
private fun validateOrderSizeSettings(realTradingConfig: RealTradingConfig) {
    val minOrderSize = realTradingConfig.minOrderSize
    val sizeStep = realTradingConfig.sizeStep

    if (minOrderSize == null || sizeStep == null) {
        logger.error {
            "実注文には min_order_size と size_step の設定が必要です。" +
                "取引所の銘柄情報（GMOコインの場合は GET /public/v1/symbols）で確認して設定してください。" +
                "min_order_size=$minOrderSize, size_step=$sizeStep"
        }
        error("実注文に必要な注文数量の設定が不足しています")
    }

    if (minOrderSize <= BigDecimal.ZERO || sizeStep <= BigDecimal.ZERO) {
        logger.error {
            "min_order_size と size_step は正の数である必要があります。" +
                "min_order_size=$minOrderSize, size_step=$sizeStep"
        }
        error("注文数量の設定が正の数ではありません")
    }
}

/**
 * 実注文に必要な価格と手数料の設定が揃っているかを検証する。
 *
 * 手数料を含めずに上限を判定すると、上限ぎりぎりの注文で実際の支払額が上限を超える。
 * 許容スリッページが無いと、想定と大きく違う価格で約定しても止まらない。
 * どちらも設定漏れを実行時ではなく起動時に気づけるようにする。
 *
 * @param realTradingConfig リアル取引設定
 * @throws IllegalStateException 手数料率または許容スリッページが未設定、もしくは値が不正な場合
 */
private fun validateOrderPriceSettings(realTradingConfig: RealTradingConfig) {
    val takerFeeRate = realTradingConfig.takerFeeRate
    val maxSlippageRate = realTradingConfig.maxSlippageRate

    if (takerFeeRate == null || maxSlippageRate == null) {
        logger.error {
            "実注文には taker_fee_rate と max_slippage_rate の設定が必要です。" +
                "手数料率は取引所の銘柄情報で確認し、許容スリッページは docs/overview/roadmap.md の" +
                "「安全ルール（数値）」に合わせてください。" +
                "taker_fee_rate=$takerFeeRate, max_slippage_rate=$maxSlippageRate"
        }
        error("実注文に必要な価格と手数料の設定が不足しています")
    }

    if (takerFeeRate < BigDecimal.ZERO) {
        logger.error { "taker_fee_rate は0以上である必要があります。taker_fee_rate=$takerFeeRate" }
        error("手数料率の設定が不正です")
    }

    if (maxSlippageRate <= BigDecimal.ZERO) {
        logger.error { "max_slippage_rate は正の数である必要があります。max_slippage_rate=$maxSlippageRate" }
        error("許容スリッページの設定が不正です")
    }
}
