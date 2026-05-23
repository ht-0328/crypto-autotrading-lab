package cryptoautotrading.presentation

import cryptoautotrading.application.RealTradingService
import cryptoautotrading.domain.model.TradeAction
import cryptoautotrading.domain.model.TradeDecision
import cryptoautotrading.domain.service.realtrading.RealTradingSafetyChecker
import cryptoautotrading.infrastructure.config.ConfigLoader
import cryptoautotrading.infrastructure.exchange.gmo.GmoPrivateApiClientAdapter
import cryptoautotrading.infrastructure.exchange.gmo.GmoPrivateApiClientImpl
import cryptoautotrading.infrastructure.exchange.gmo.GmoPublicApiClient
import cryptoautotrading.infrastructure.exchange.gmo.auth.EnvGmoCredentialProvider
import cryptoautotrading.infrastructure.exchange.gmo.auth.GmoSignatureGeneratorImpl
import cryptoautotrading.infrastructure.output.StateRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Paths
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

/**
 * リアル買い注文を手動確認するための専用メインクラス
 */
fun main() = runBlocking {
    logger.info { "【警告】実際のお金を使うリアル買い注文の手動確認モードを開始します！" }

    val apiKey = System.getenv("GMO_API_KEY")
    val apiSecret = System.getenv("GMO_API_SECRET")

    if (apiKey.isNullOrBlank() || apiSecret.isNullOrBlank()) {
        logger.error { "環境変数 GMO_API_KEY または GMO_API_SECRET が設定されていません。手動確認を中止します。" }
        exitProcess(1)
    }

    val config = try {
        ConfigLoader.load()
    } catch (e: Exception) {
        logger.error(e) { "設定ファイルの読み込みに失敗しました" }
        exitProcess(1)
    }

    val realTradingConfig = config.realTrading
    if (realTradingConfig.dryRun) {
        logger.error { "dry_run が true です。実注文を手動確認する場合は false に設定してください。手動確認を中止します。" }
        exitProcess(1)
    }
    if (!realTradingConfig.realTradeEnabled) {
        logger.error { "real_trade_enabled が false です。実注文を手動確認する場合は true に設定してください。手動確認を中止します。" }
        exitProcess(1)
    }

    // データディレクトリとStateRepositoryの初期化
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
    val statePath = Paths.get(finalDir, config.output.statePath).toString()
    val stateRepository = StateRepository(statePath)
    var currentState = stateRepository.load()

    val publicBaseUrl = config.api.publicBaseUrl ?: "https://api.coin.z.com/public"
    val privateBaseUrl = config.api.privateBaseUrl ?: "https://api.coin.z.com/private"
    val retryCount = config.api.retryCount

    HttpClient(CIO).use { httpClient ->
        val privateApiClient = GmoPrivateApiClientImpl(
            httpClient = httpClient,
            baseUrl = privateBaseUrl,
            signatureGenerator = GmoSignatureGeneratorImpl(),
            credentialProvider = EnvGmoCredentialProvider()
        )
        val realTradingExchangeClient = GmoPrivateApiClientAdapter(privateApiClient)
        val safetyChecker = RealTradingSafetyChecker()
        val realTradingService = RealTradingService(
            exchangeClient = realTradingExchangeClient,
            safetyChecker = safetyChecker
        )

        GmoPublicApiClient(publicBaseUrl, retryCount).use { apiClient ->
            try {
                // 現在価格の取得
                val symbol = config.trading.symbol
                logger.info { "現在価格を取得します (symbol=$symbol)..." }
                val ticker = apiClient.getTicker(symbol)
                val currentPrice = ticker.data.first().last.toBigDecimal()
                logger.info { "現在価格: $currentPrice" }

                // 手動確認用の判定作成
                val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "手動確認用買いシグナル")
                val tradeAmount = config.trading.tradeAmount

                logger.info { "RealTradingService を経由して注文処理を実行します。予定金額: $tradeAmount JPY" }

                // 注文実行
                currentState = realTradingService.executeOrderIfNeeded(
                    decision = decision,
                    config = realTradingConfig,
                    tradeAmount = tradeAmount,
                    symbol = symbol,
                    currentState = currentState,
                    currentPrice = currentPrice
                )

                // 状態の保存
                stateRepository.save(currentState)

                val orderId = currentState.realTrading.latestOrder?.orderId
                if (!orderId.isNullOrBlank()) {
                    logger.info { "手動確認終了: 注文処理が実行され、state.json に latestOrder.orderId ($orderId) が保存されました。" }
                } else {
                    logger.warn { "手動確認終了: 注文処理は実行されましたが、orderId が保存されていません（安全チェック等で見送られた可能性があります）。" }
                }

            } catch (e: Exception) {
                logger.error(e) { "手動確認中にエラーが発生しました" }
                exitProcess(1)
            }
        }
    }
}
