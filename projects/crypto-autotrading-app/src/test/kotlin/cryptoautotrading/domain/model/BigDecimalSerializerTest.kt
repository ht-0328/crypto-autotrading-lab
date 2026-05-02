package cryptoautotrading.domain.model

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@Serializable
data class TestState(
    @Serializable(with = BigDecimalSerializer::class)
    val num: BigDecimal
)

class BigDecimalSerializerTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `test serialization and deserialization with high precision`() {
        // Arrange
        val originalValue = BigDecimal("0.123456789123456789")
        val state = TestState(num = originalValue)

        // Act
        val jsonString = json.encodeToString(state)
        val decodedState = json.decodeFromString<TestState>(jsonString)

        // Assert
        // 文字列としてシリアライズされることを確認
        assertEquals("""{"num":"0.123456789123456789"}""", jsonString)
        // 精度が落ちていないことを確認
        assertEquals(originalValue, decodedState.num)
    }

    @Test
    fun `test deserialization from existing legacy numeric JSON`() {
        // Arrange
        val jsonString = """{"num": 123.45}"""

        // Act
        val decodedState = json.decodeFromString<TestState>(jsonString)

        // Assert
        assertEquals(BigDecimal("123.45"), decodedState.num)
    }

    @Test
    fun `test deserialization from string formatted JSON`() {
        // Arrange
        val jsonString = """{"num": "123.456789"}"""

        // Act
        val decodedState = json.decodeFromString<TestState>(jsonString)

        // Assert
        assertEquals(BigDecimal("123.456789"), decodedState.num)
    }
}
