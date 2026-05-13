#!/bin/bash
cd /app/projects/crypto-autotrading-app

# 1. TradingApplication.kt: Insert RealTradeService correctly and handle the call.
sed -i 's/import cryptoautotrading.domain.simulation.SimulationService/import cryptoautotrading.domain.simulation.SimulationService\nimport cryptoautotrading.domain.simulation.RealTradeService\nimport cryptoautotrading.domain.model.TradeAction/' src/main/kotlin/cryptoautotrading/application/TradingApplication.kt

sed -i 's/private val simulationService: SimulationService,/private val simulationService: SimulationService,\n    private val realTradeService: RealTradeService,/' src/main/kotlin/cryptoautotrading/application/TradingApplication.kt

sed -i '/stateRepository.save(nextState)/i\
            if (decision.action == TradeAction.BUY_CANDIDATE) {\
                realTradeService.processRealOrder(nextState, currentPrice)\
            }' src/main/kotlin/cryptoautotrading/application/TradingApplication.kt

# 2. Main.kt: Supply the right arguments, import missing stuff.
cat << 'KOTLIN' > src/main/kotlin/cryptoautotrading/presentation/Main.kt
package cryptoautotrading.presentation

import cryptoautotrading.application.TradingApplication
import cryptoautotrading.domain.simulation.ProfitAndLossCalculator
import cryptoautotrading.domain.simulation.SimulationService
import cryptoautotrading.domain.simulation.RealTradeService
import cryptoautotrading.infrastructure.config.ConfigLoader
import cryptoautotrading.infrastructure.exchange.gmo.GmoPublicApiClient
import cryptoautotrading.infrastructure.exchange.gmo.GmoPrivateApiClient
import cryptoautotrading.infrastructure.secret.EnvVarSecretManager
import cryptoautotrading.infrastructure.output.ConsoleOutput
import cryptoautotrading.infrastructure.output.CsvRepository
import cryptoautotrading.infrastructure.output.StateRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking

fun main(): Unit = runBlocking {
    val logger = KotlinLogging.logger {}
    try {
        val config = ConfigLoader.load()
        val marketDataClient = GmoPublicApiClient(config.api.baseUrl)
        val stateRepository = StateRepository(config.output.statePath)
        val pnlCalculator = ProfitAndLossCalculator()
        val resultOutputPort = ConsoleOutput()
        val tradeHistoryRepository = CsvRepository(config.output.outputPath)

        val secretManager = EnvVarSecretManager()
        val exchangeOrderClient = GmoPrivateApiClient(config.api.baseUrl)
        val realTradeService = RealTradeService(config.trading, secretManager, exchangeOrderClient)

        val simulationService = SimulationService(
            config.trading,
            pnlCalculator
        )

        val app = TradingApplication(
            marketDataClient = marketDataClient,
            config = config,
            stateRepository = stateRepository,
            resultOutputPort = resultOutputPort,
            tradeHistoryRepository = tradeHistoryRepository,
            pnlCalculator = pnlCalculator,
            simulationService = simulationService,
            realTradeService = realTradeService
        )

        app.run()

    } catch (e: Exception) {
        logger.error(e) { "エラー" }
    }
}
KOTLIN

# 3. GmoPrivateApiClient missing jackson import
sed -i 's/import io.ktor.serialization.jackson.\*/import io.ktor.serialization.jackson.*\nimport io.ktor.serialization.jackson.jackson/' src/main/kotlin/cryptoautotrading/infrastructure/exchange/gmo/GmoPrivateApiClient.kt
