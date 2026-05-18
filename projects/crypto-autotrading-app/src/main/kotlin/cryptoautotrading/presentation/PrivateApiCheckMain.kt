package cryptoautotrading.presentation

import cryptoautotrading.infrastructure.config.ConfigLoader
import cryptoautotrading.infrastructure.exchange.gmo.GmoPrivateApiClientImpl
import cryptoautotrading.infrastructure.exchange.gmo.auth.EnvGmoCredentialProvider
import cryptoautotrading.infrastructure.exchange.gmo.auth.GmoSignatureGeneratorImpl
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

/**
 * GMO Private API 疎通確認用メインクラス
 */
fun main() = runBlocking {
    logger.info { "Private API疎通確認を開始します" }

    val apiKey = System.getenv("GMO_API_KEY")
    val apiSecret = System.getenv("GMO_API_SECRET")

    if (apiKey.isNullOrBlank() || apiSecret.isNullOrBlank()) {
        logger.error { "環境変数 GMO_API_KEY または GMO_API_SECRET が設定されていません。設定して再実行してください。" }
        exitProcess(1)
    }

    val config = try {
        ConfigLoader.load()
    } catch (e: Exception) {
        logger.error(e) { "設定ファイルの読み込みに失敗しました" }
        exitProcess(1)
    }

    val baseUrl = config.api.privateBaseUrl ?: config.api.baseUrl ?: "https://api.coin.z.com"
    logger.info { "使用するベースURL: $baseUrl" }

    val httpClient = HttpClient(CIO) {
        // 設定が必要なら追加
    }

    try {
        val credentialProvider = EnvGmoCredentialProvider()
        val signatureGenerator = GmoSignatureGeneratorImpl()

        val client = GmoPrivateApiClientImpl(
            httpClient = httpClient,
            baseUrl = baseUrl,
            signatureGenerator = signatureGenerator,
            credentialProvider = credentialProvider
        )

        // 1. 残高取得API呼び出し
        logger.info { "残高取得APIを呼び出します..." }
        val assets = client.getAssets()
        logger.info { "残高取得に成功しました" }

        val jpyAsset = assets.find { it.symbol == "JPY" }
        if (jpyAsset != null) {
            logger.info { "JPYの利用可能残高: ${jpyAsset.available}" }
        } else {
            logger.info { "JPYの資産情報が見つかりませんでした" }
        }

        // 2. 未約定注文取得API呼び出し
        val symbol = "BTC" // 一旦BTCで確認
        logger.info { "未約定注文取得APIを呼び出します (symbol=$symbol)..." }
        val activeOrders = client.getActiveOrders(symbol)
        logger.info { "未約定注文取得に成功しました" }
        logger.info { "未約定注文の件数: ${activeOrders.size} 件" }

    } catch (e: Exception) {
        logger.error(e) { "Private APIの呼び出し中にエラーが発生しました" }
    } finally {
        httpClient.close()
        logger.info { "Private API疎通確認を終了します" }
    }
}
