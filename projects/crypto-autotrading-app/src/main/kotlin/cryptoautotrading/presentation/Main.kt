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
    logger.info { "Crypto Auto-Trading Lab 起動処理を開始します" }

    try {
        // 設定を読み込む
        logger.info { "設定ファイルの読み込みを開始します" }
        val config = ConfigLoader.load()
        logger.info { "設定ファイルの読み込みが完了しました" }

        // APIのベースURLを設定ファイルから取得する
        val baseUrl = config.api.baseUrl ?: "https://api.coin.z.com"
        logger.info { "最終的に採用したAPIベースURL: $baseUrl" }

        GmoPublicApiClient(baseUrl).use { apiClient ->
            val app = TradingApplication(config, apiClient)

            logger.info { "TradingApplication の実行を開始します" }
            app.run()
            logger.info { "TradingApplication の実行が終了しました" }
        }
    } catch (e: Exception) {
        logger.error(e) { "アプリケーションの起動・実行中に予期せぬエラーが発生しました: ${e.message}" }
    } finally {
        logger.info { "Crypto Auto-Trading Lab 起動処理が終了しました" }
    }
}
