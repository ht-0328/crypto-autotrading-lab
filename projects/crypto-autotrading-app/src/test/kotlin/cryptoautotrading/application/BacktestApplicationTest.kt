package cryptoautotrading.application

import cryptoautotrading.domain.backtest.BacktestResult
import cryptoautotrading.domain.model.Kline
import cryptoautotrading.domain.repository.BacktestResultOutputPort
import cryptoautotrading.domain.repository.KlineCsvReader
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BacktestApplicationTest {

    @Test
    fun `必須パラメータが不足している場合はIllegalArgumentExceptionをスローすること`() {
        val application = BacktestApplication(mockk(), mockk())

        assertThrows<IllegalArgumentException> {
            application.run(null, "strategy", "1000", "out1", "out2")
        }
    }

    @Test
    fun `正常な入力パラメータの場合はバックテストが実行され結果が出力されること`() {
        // Arrange
        val mockReader = mockk<KlineCsvReader>()
        val mockOutputPort = mockk<BacktestResultOutputPort>(relaxed = true)

        every { mockReader.read(any()) } returns listOf(
            Kline("202605010000", "100", "110", "90", "105", "10")
        )

        val application = BacktestApplication(mockReader, mockOutputPort)

        // Act
        application.run(
            klineCsvPath = "dummy.csv",
            strategyName = "SafeReboundStrategy",
            initialCapitalStr = "10000",
            summaryOutputPath = "summary.csv",
            stepsOutputPath = "steps.csv"
        )

        // Assert
        verify { mockReader.read("dummy.csv") }
        verify { mockOutputPort.output(any(), "summary.csv", "steps.csv") }
    }
}
