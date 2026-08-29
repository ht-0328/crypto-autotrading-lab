package cryptoautotrading.mocks

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.math.BigDecimal

/**
 * WireMock のK線スタブが、実際の取引所が返しうるデータになっているかを検査する。
 *
 * スタブが壊れていても CI の結合テストは出力ファイルの存在だけで通ってしまい、
 * 気付くのが遅れる。値の整合性はここでビルド時に落とす。
 * 開始時刻はテンプレートで実行時に決まるため、ここでは検査しない（CI が確認する）。
 */
class WireMockKlineStubTest {

    private val stubFile = File("../../mocks/wiremock/mappings/klines.json")

    /** SafeReboundStrategy が判定に必要とする本数 */
    private val requiredKlineCount = 12

    private fun loadKlines(): List<Map<String, String>> {
        val stub = Json.parseToJsonElement(stubFile.readText()).jsonObject
        val body = stub["response"]!!.jsonObject["body"]!!.jsonPrimitive.content
        // 先頭のテンプレート宣言（複数ある）を取り除いて JSON 本体だけを読む
        val jsonBody = body.substringAfterLast(ASSIGN_END_MARKER)
        return Json.parseToJsonElement(jsonBody).jsonObject["data"]!!.jsonArray.map { element ->
            element.jsonObject.mapValues { it.value.jsonPrimitive.content }
        }
    }

    @Test
    fun `スタブのファイルが存在すること`() {
        assertTrue(stubFile.exists(), "スタブが見つかりません: ${stubFile.absolutePath}")
    }

    @Test
    fun `判定に必要な本数のK線が定義されていること`() {
        val klines = loadKlines()

        assertTrue(
            klines.size >= requiredKlineCount,
            "K線が ${klines.size} 本しかありません。Strategy は $requiredKlineCount 本必要です"
        )
    }

    @Test
    fun `すべてのK線で高値と安値の整合性が取れていること`() {
        loadKlines().forEachIndexed { index, kline ->
            val open = BigDecimal(kline.getValue("open"))
            val high = BigDecimal(kline.getValue("high"))
            val low = BigDecimal(kline.getValue("low"))
            val close = BigDecimal(kline.getValue("close"))

            assertTrue(high >= open.max(close), "$index 本目: 高値 $high が始値・終値を下回っています")
            assertTrue(low <= open.min(close), "$index 本目: 安値 $low が始値・終値を上回っています")
            assertTrue(high >= low, "$index 本目: 高値 $high が安値 $low を下回っています")
        }
    }

    @Test
    fun `すべてのK線で価格が正の数であること`() {
        loadKlines().forEachIndexed { index, kline ->
            listOf("open", "high", "low", "close").forEach { key ->
                val price = BigDecimal(kline.getValue(key))
                assertTrue(price > BigDecimal.ZERO, "$index 本目: $key が0以下です")
            }
        }
    }

    @Test
    fun `開始時刻がテンプレートで実行時刻に追従すること`() {
        val body = templateBody()

        // 各足で now を評価すると描画のタイミングで間隔がずれるため、基点は1回だけ評価する
        assertEquals(1, Regex("\\{\\{now").findAll(body).count(), "now の評価は1回だけにしてください")
        assertTrue(body.contains("{{#assign 'base'}}"), "基点を base として保持してください")
    }

    @Test
    fun `開始時刻が刻みの境界に切り下げられること`() {
        val body = templateBody()

        // アプリは前営業日分と当営業日分を別々に取得する。2回のリクエストは別々に描画され、
        // now がミリ秒単位でずれる。境界に切り下げないと、つなげたときに間隔が合わなくなる
        assertTrue(
            body.contains("'%' $FIVE_MINUTES_MILLIS"),
            "基点を5分の境界に切り下げてください: $body"
        )
    }

    /** スタブのレスポンス本文（テンプレートを含む）を返す */
    private fun templateBody(): String = Json.parseToJsonElement(stubFile.readText())
        .jsonObject["response"]!!.jsonObject["body"]!!.jsonPrimitive.content

    private companion object {
        /** 基点を宣言するテンプレートブロックの終わり */
        const val ASSIGN_END_MARKER = "{{/assign}}"

        /** 5分のミリ秒 */
        const val FIVE_MINUTES_MILLIS = 300000
    }
}
