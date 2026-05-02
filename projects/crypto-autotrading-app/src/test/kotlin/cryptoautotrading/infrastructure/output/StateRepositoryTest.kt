package cryptoautotrading.infrastructure.output

import cryptoautotrading.domain.model.SimulationState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class StateRepositoryTest {

    @Test
    fun `状態の保存と読み込みが正しく行われること`(@TempDir tempDir: Path) {
        // Arrange
        val stateFilePath = tempDir.resolve("state.json").toAbsolutePath().toString()
        val repository = StateRepository(stateFilePath)
        val state = SimulationState(
            isHolding = true,
            buyPrice = 50000.0,
            holdingAmount = 0.5,
            lastUpdatedAt = "2023-01-01T00:00:00"
        )

        // Act
        repository.save(state)
        val loadedState = repository.load()

        // Assert
        assertTrue(loadedState.isHolding)
        assertEquals(50000.0, loadedState.buyPrice)
        assertEquals(0.5, loadedState.holdingAmount)
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
        assertFalse(loadedState.isHolding)
        assertEquals(0.0, loadedState.buyPrice)
        assertEquals(0.0, loadedState.holdingAmount)
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
