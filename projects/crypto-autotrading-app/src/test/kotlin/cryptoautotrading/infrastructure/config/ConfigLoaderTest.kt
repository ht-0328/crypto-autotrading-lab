package cryptoautotrading.infrastructure.config

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ConfigLoaderTest {

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `load should map yaml to AppConfig correctly`(@TempDir tempDir: Path) {
        // Arrange
        val yamlContent = """
            app:
              interval: "5min"
            trading:
              symbol: "BTC"
              initial_capital: 10000
              trade_amount: 1000
              buy_threshold: 0.005
              sell_threshold: 0.005
              volatility_threshold: 0.003
              sharp_change_threshold: 0.01
            api:
              retry_count: 3
              base_url: "https://api.coin.z.com"
            output:
              output_path: "trades.csv"
              state_path: "state.json"
        """.trimIndent()
        val configFile = tempDir.resolve("application-test.yaml").toFile()
        configFile.writeText(yamlContent)

        // Act
        val mapperField = ConfigLoader::class.java.getDeclaredField("mapper")
        mapperField.isAccessible = true
        val mapper = mapperField.get(ConfigLoader) as com.fasterxml.jackson.databind.ObjectMapper

        val config = mapper.readValue(configFile, cryptoautotrading.domain.model.AppConfig::class.java)

        // Assert
        assertEquals("5min", config.app.interval)
        assertEquals("BTC", config.trading.symbol)
        assertEquals(10000, config.trading.initialCapital)
        assertEquals(1000, config.trading.tradeAmount)
        assertEquals(0.005, config.trading.buyThreshold)
        assertEquals(0.005, config.trading.sellThreshold)
        assertEquals(0.003, config.trading.volatilityThreshold)
        assertEquals(0.01, config.trading.sharpChangeThreshold)
        assertEquals(3, config.api.retryCount)
        assertEquals("https://api.coin.z.com", config.api.baseUrl)
        assertEquals("trades.csv", config.output.outputPath)
        assertEquals("state.json", config.output.statePath)
    }
}
