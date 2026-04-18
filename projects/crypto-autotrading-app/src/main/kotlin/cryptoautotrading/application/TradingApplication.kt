package cryptoautotrading.application

import cryptoautotrading.domain.model.AppConfig
import cryptoautotrading.domain.strategy.TradingStrategy
import cryptoautotrading.infrastructure.exchange.gmo.GmoPublicApiClient
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * トレーディングアプリケーションのメインロジックを実行するクラス
 *
 * @property config アプリケーション設定
 * @property apiClient GMOパブリックAPIクライアント
 */
class TradingApplication(
    private val config: AppConfig,
    private val apiClient: GmoPublicApiClient
) {

    private val logger = KotlinLogging.logger {}

    /**
     * アプリケーションの実行を開始する
     */
    suspend fun run() {
        try {
            logger.info { "Running trading application with config: $config" }

            val tickerResponse = apiClient.getTicker(config.trading.symbol)
            logger.info { "Ticker Response: $tickerResponse" }

            val klineResponse = apiClient.getKlines(config.trading.symbol, config.app.interval, "20231001")
            logger.info { "Klines Response: $klineResponse" }

            val strategy = TradingStrategy()
            val decision = strategy.judge(klineResponse.data, false)
            logger.info { "Trade Decision: ${decision.action.description}, Reason: ${decision.reason}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to get data from API" }
        }
    }
}
