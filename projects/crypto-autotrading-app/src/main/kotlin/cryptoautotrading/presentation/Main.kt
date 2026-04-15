package cryptoautotrading.presentation

import cryptoautotrading.application.TradingApplication
import cryptoautotrading.infrastructure.exchange.gmo.GmoPublicApiClient
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking

private val logger = KotlinLogging.logger {}

fun main() = runBlocking {
    logger.info { "Hello, Crypto Auto-Trading Lab!" }

    try {
        // Use wiremock hostname when running in docker compose, or localhost when running on host.
        val wiremockUrl = System.getenv("WIREMOCK_URL") ?: "http://localhost:8080"

        GmoPublicApiClient(wiremockUrl).use { apiClient ->
            val app = TradingApplication(apiClient)

            app.run()
        }
    } catch (e: Exception) {
        logger.error(e) { "Failed to start the application" }
    }
}
