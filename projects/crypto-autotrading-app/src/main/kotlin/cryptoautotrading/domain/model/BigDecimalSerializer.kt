package cryptoautotrading.domain.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal

/**
 * BigDecimalをJSONシリアライズ/デシリアライズするためのカスタムシリアライザ
 */
object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)

    /**
     * BigDecimalをシリアライズする
     * @param encoder エンコーダー
     * @param value BigDecimalの値
     */
    override fun serialize(encoder: Encoder, value: BigDecimal) {
        // Kotlinx.serialization の JsonEncoder では、精度を落とさないためにStringとして出力する
        encoder.encodeString(value.toPlainString())
    }

    /**
     * BigDecimalをデシリアライズする
     * @param decoder デコーダー
     * @return デシリアライズされたBigDecimal
     */
    override fun deserialize(decoder: Decoder): BigDecimal {
        // JsonDecoderの場合、文字列要素からより正確なBigDecimalを生成する
        if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            return BigDecimal(element.jsonPrimitive.content)
        }
        // 他のフォーマットなどJsonDecoder以外の場合はString経由でデコード
        return BigDecimal(decoder.decodeString())
    }
}
