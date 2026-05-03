package cryptoautotrading.application

import cryptoautotrading.domain.model.AppConfig
import cryptoautotrading.domain.model.AppSettings
import cryptoautotrading.domain.model.ApiConfig
import cryptoautotrading.domain.model.OutputConfig
import cryptoautotrading.domain.model.TradingConfig
import cryptoautotrading.domain.strategy.SafeReboundStrategy
import cryptoautotrading.domain.strategy.SimpleContrarianStrategy
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TradingApplicationTest {

    private fun createAppConfig(strategyName: String): AppConfig {
        return AppConfig(
            app = AppSettings(interval = "5min"),
            trading = TradingConfig(
                strategyName = strategyName,
                symbol = "BTC",
                initialCapital = 10000,
                tradeAmount = 1000,
                buyThreshold = 0.05,
                sellThreshold = 0.05,
                volatilityThreshold = 0.003,
                sharpChangeThreshold = 0.01
            ),
            api = ApiConfig(retryCount = 3, baseUrl = ""),
            output = OutputConfig(outputPath = "", statePath = "")
        )
    }

    // Access private method via reflection for testing
    private fun invokeCreateStrategy(app: TradingApplication, config: TradingConfig): Any {
        val method = TradingApplication::class.java.getDeclaredMethod("createStrategy", TradingConfig::class.java)
        method.isAccessible = true
        return method.invoke(app, config)
    }

    @Test
    fun `strategyName = SafeReboundStrategy で SafeReboundStrategy が作られること`() {
        val config = createAppConfig("SafeReboundStrategy")
        val app = TradingApplication(
            config = config,
            marketDataClient = mockk(),
            stateRepository = mockk(),
            tradeHistoryRepository = mockk(),
            resultOutputPort = mockk()
        )

        val strategy = invokeCreateStrategy(app, config.trading)
        assertTrue(strategy is SafeReboundStrategy)
    }

    @Test
    fun `strategyName = SimpleContrarianStrategy で SimpleContrarianStrategy が作られること`() {
        val config = createAppConfig("SimpleContrarianStrategy")
        val app = TradingApplication(
            config = config,
            marketDataClient = mockk(),
            stateRepository = mockk(),
            tradeHistoryRepository = mockk(),
            resultOutputPort = mockk()
        )

        val strategy = invokeCreateStrategy(app, config.trading)
        assertTrue(strategy is SimpleContrarianStrategy)
    }

    @Test
    fun `未対応の strategyName では例外になること`() {
        val config = createAppConfig("UnknownStrategy")
        val app = TradingApplication(
            config = config,
            marketDataClient = mockk(),
            stateRepository = mockk(),
            tradeHistoryRepository = mockk(),
            resultOutputPort = mockk()
        )

        val exception = assertThrows<Exception> {
            invokeCreateStrategy(app, config.trading)
        }

        val cause = exception.cause ?: exception
        assertTrue(cause.message!!.contains("Unknown strategyName: UnknownStrategy. Supported strategies: SafeReboundStrategy, SimpleContrarianStrategy"))
    }
}
