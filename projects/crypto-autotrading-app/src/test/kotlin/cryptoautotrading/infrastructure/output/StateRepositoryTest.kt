package cryptoautotrading.infrastructure.output

import cryptoautotrading.domain.model.SimulationState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.math.BigDecimal
import java.nio.file.Path

class StateRepositoryTest {

    @Test
    fun `状態の保存と読み込みが正しく行われること`(@TempDir tempDir: Path) {
        // Arrange
        val stateFilePath = tempDir.resolve("state.json").toAbsolutePath().toString()
        val repository = StateRepository(stateFilePath)
        val state = SimulationState(
            cashBalance = BigDecimal("150000.00"),
            isHolding = true,
            buyPrice = BigDecimal("10345678.12345678"),
            holdingAmount = BigDecimal("0.00009665"),
            realizedProfitAndLoss = BigDecimal("1234.56"),
            lastUpdatedAt = "2023-01-01T00:00:00"
        )

        // Act
        repository.save(state)
        val loadedState = repository.load()

        // Assert
        assertEquals(BigDecimal("150000.00"), loadedState.cashBalance)
        assertTrue(loadedState.isHolding)
        assertEquals(BigDecimal("10345678.12345678"), loadedState.buyPrice)
        assertEquals(BigDecimal("0.00009665"), loadedState.holdingAmount)
        assertEquals(BigDecimal("1234.56"), loadedState.realizedProfitAndLoss)
        assertEquals("2023-01-01T00:00:00", loadedState.lastUpdatedAt)
    }

    @Test
    fun `古い数値形式のJSONでも正しく読み込めること`(@TempDir tempDir: Path) {
        // Arrange
        val stateFile = tempDir.resolve("legacy_numeric_state.json").toFile()
        stateFile.writeText(
            """
            {
              "isHolding": true,
              "buyPrice": 10000000.5,
              "holdingAmount": 0.0001,
              "lastUpdatedAt": "2023-01-01T00:00:00"
            }
            """.trimIndent()
        )
        val repository = StateRepository(stateFile.absolutePath)

        // Act
        val loadedState = repository.load()

        // Assert
        assertTrue(loadedState.isHolding)
        assertEquals(BigDecimal("10000000.5"), loadedState.buyPrice)
        assertEquals(BigDecimal("0.0001"), loadedState.holdingAmount)
        assertEquals("2023-01-01T00:00:00", loadedState.lastUpdatedAt)
    }

    @Test
    fun `ファイルが存在しない場合は初期状態を返すこと`(@TempDir tempDir: Path) {
        // Arrange
        val stateFilePath = tempDir.resolve("non_existent_state.json").toAbsolutePath().toString()
        val repository = StateRepository(stateFilePath)

        // Act
        val loadedState = repository.load()

        // Assert
        assertEquals(BigDecimal.ZERO, loadedState.cashBalance)
        assertFalse(loadedState.isHolding)
        assertEquals(BigDecimal.ZERO, loadedState.buyPrice)
        assertEquals(BigDecimal.ZERO, loadedState.holdingAmount)
        assertEquals(BigDecimal.ZERO, loadedState.realizedProfitAndLoss)
    }

    @Test
    fun `ファイルが不正なJSONの場合は例外が発生すること`(@TempDir tempDir: Path) {
        // Arrange
        val stateFile = tempDir.resolve("invalid_state.json").toFile()
        stateFile.writeText("{ invalid json }")
        val repository = StateRepository(stateFile.absolutePath)

        // Act & Assert
        assertThrows(Exception::class.java) {
            repository.load()
        }
    }
}
