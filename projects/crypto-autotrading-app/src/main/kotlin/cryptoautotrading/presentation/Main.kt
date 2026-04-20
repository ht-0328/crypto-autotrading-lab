package cryptoautotrading.presentation

import cryptoautotrading.application.TradingApplication
import cryptoautotrading.infrastructure.config.ConfigLoader
import cryptoautotrading.infrastructure.exchange.gmo.GmoPublicApiClient
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking

private val logger = KotlinLogging.logger {}

/**
 * アプリケーションのエントリーポイント
 */
fun main() = runBlocking {
    logger.info { "Hello, Crypto Auto-Trading Lab!" }

    try {
        // 設定を読み込む
        val config = ConfigLoader.load()
        logger.info { "設定の読み込みが完了しました。" }

        // APIのベースURLを取得する。環境変数(API_BASE_URL)、設定ファイル、デフォルト値の順に優先する
        val baseUrl = System.getenv("API_BASE_URL") ?: config.api.baseUrl ?: "https://api.coin.z.com"

        GmoPublicApiClient(baseUrl).use { apiClient ->
            val app = TradingApplication(config, apiClient)

            app.run()
        }
    } catch (e: Exception) {
        logger.error(e) { "Failed to start the application" }
    }
}
