package cryptoautotrading.domain.model

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.DOUBLE)

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder.encodeDouble(value.toDouble())
    }

    override fun deserialize(decoder: Decoder): BigDecimal {
        if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            return BigDecimal(element.jsonPrimitive.content)
        }
        return BigDecimal.valueOf(decoder.decodeDouble())
    }
}

@Serializable
data class TestState(
    @Serializable(with = BigDecimalSerializer::class)
    val num: BigDecimal
)

class BigDecimalSerializerTest {
    @Test
    fun testSerialization() {
        val json = Json { ignoreUnknownKeys = true }
        val s = json.decodeFromString<TestState>("""{"num": 123.45}""")
        assertEquals(BigDecimal("123.45"), s.num)

        val str = json.encodeToString(s)
        println(str)
        val s2 = json.decodeFromString<TestState>(str)
        assertEquals(s.num, s2.num)
    }
}
