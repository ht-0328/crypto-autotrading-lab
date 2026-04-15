package cryptoautotrading.application

import cryptoautotrading.infrastructure.exchange.gmo.GmoPublicApiClient
import io.github.oshai.kotlinlogging.KotlinLogging

class TradingApplication(private val apiClient: GmoPublicApiClient) {

    private val logger = KotlinLogging.logger {}

    suspend fun run() {
        try {
            val tickerResponse = apiClient.getTicker("BTC")
            logger.info { "Ticker Response: $tickerResponse" }

            val klineResponse = apiClient.getKlines("BTC", "5min", "20231001")
            logger.info { "Klines Response: $klineResponse" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to get data from API" }
        }
    }
}
