package cryptoautotrading.infrastructure.output

import cryptoautotrading.domain.model.SimulationState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class StateRepositoryTest {

    @Test
    fun `save and load should persist and retrieve state correctly`(@TempDir tempDir: Path) {
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
    fun `load should return default state when file does not exist`(@TempDir tempDir: Path) {
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
}
