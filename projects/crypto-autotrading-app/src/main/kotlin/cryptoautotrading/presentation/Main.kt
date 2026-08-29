package cryptoautotrading.presentation

import cryptoautotrading.application.TradingApplication
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
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

/**
 * 実注文が許可される最小のフェーズ。
 * ロードマップ上、実注文は Phase3（通知 → 手動承認 → 実注文）のスコープ。
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
