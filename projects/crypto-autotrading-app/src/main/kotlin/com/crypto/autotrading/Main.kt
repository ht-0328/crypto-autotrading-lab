package com.crypto.autotrading

import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.URI

private val logger = KotlinLogging.logger {}

fun main() {
    logger.info { "Hello, Crypto Auto-Trading Lab!" }

    try {
        // Use wiremock hostname when running in docker compose, or localhost when running on host.
        val wiremockUrl = System.getenv("WIREMOCK_URL") ?: "http://localhost:8080"

        val tickerUrl = "$wiremockUrl/public/v1/ticker?symbol=BTC"
        val tickerBody = URI(tickerUrl).toURL().readText()
        logger.info { "Ticker Response: $tickerBody" }

        val klinesUrl = "$wiremockUrl/public/v1/klines?symbol=BTC&interval=5min&date=20231001"
        val klinesBody = URI(klinesUrl).toURL().readText()
        logger.info { "Klines Response: $klinesBody" }
    } catch (e: Exception) {
        logger.error(e) { "Failed to connect to WireMock" }
    }
}
