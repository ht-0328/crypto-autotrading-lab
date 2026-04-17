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

        // Docker Composeで実行する場合はwiremockのホスト名を、ホストで実行する場合はlocalhostを使用する
        val wiremockUrl = System.getenv("WIREMOCK_URL") ?: "http://localhost:8080"

        GmoPublicApiClient(wiremockUrl).use { apiClient ->
            val app = TradingApplication(config, apiClient)

            app.run()
        }
    } catch (e: Exception) {
        logger.error(e) { "Failed to start the application" }
    }
}
