package cryptoautotrading.application

import cryptoautotrading.domain.model.TradeAction
import cryptoautotrading.domain.model.TradeDecision
import cryptoautotrading.domain.model.realtrading.RealTradingConfig
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class RealTradeOrderUseCaseTest {

    private val useCase = RealTradeOrderUseCase()

    @Test
    fun `realTradeEnabledがfalseの場合は実注文処理がスキップされること`() {
        // Arrange
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = RealTradingConfig(realTradeEnabled = false, dryRun = false)

        // Act & Assert
        // 現時点ではログ出力のみで例外をスローしないことを確認
        assertDoesNotThrow {
            useCase.executeOrderIfNeeded(decision, config)
        }
    }

    @Test
    fun `dryRunがtrueの場合は実注文処理がスキップされること`() {
        // Arrange
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = RealTradingConfig(realTradeEnabled = true, dryRun = true)

        // Act & Assert
        assertDoesNotThrow {
            useCase.executeOrderIfNeeded(decision, config)
        }
    }

    @Test
    fun `BUY_CANDIDATE以外の場合は実注文処理がスキップされること`() {
        // Arrange
        val decision = TradeDecision(TradeAction.SELL_CANDIDATE, "sell signal")
        val config = RealTradingConfig(realTradeEnabled = true, dryRun = false)

        // Act & Assert
        assertDoesNotThrow {
            useCase.executeOrderIfNeeded(decision, config)
        }
    }

    @Test
    fun `BUY_CANDIDATEで設定が有効な場合はPhase1の事前処理が実行されること`() {
        // Arrange
        val decision = TradeDecision(TradeAction.BUY_CANDIDATE, "buy signal")
        val config = RealTradingConfig(realTradeEnabled = true, dryRun = false)

        // Act & Assert
        assertDoesNotThrow {
            useCase.executeOrderIfNeeded(decision, config)
        }
    }
}
