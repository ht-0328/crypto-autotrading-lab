package cryptoautotrading.infrastructure.exchange.gmo

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class HttpRetryPolicyTest {

    /** ばらつきを常に最大にする乱数。待ち時間の上限を確かめるために使う */
    private val maxJitterRandom = object : Random() {
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextLong(until: Long): Long = until - 1
    }

    /** ばらつきを常に0にする乱数。待ち時間の下限を確かめるために使う */
    private val noJitterRandom = object : Random() {
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextLong(until: Long): Long = 0
    }

    private fun policy(
        maxAttempts: Int = 4,
        baseDelayMillis: Long = 500,
        maxDelayMillis: Long = 8000,
        random: Random = noJitterRandom
    ) = HttpRetryPolicy(maxAttempts, baseDelayMillis, maxDelayMillis, random)

    @Test
    fun `最大試行回数に達するまでは再試行できること`() {
        val target = policy(maxAttempts = 3)

        assertTrue(target.canRetry(1))
        assertTrue(target.canRetry(2))
    }

    @Test
    fun `最大試行回数に達したら再試行できないこと`() {
        val target = policy(maxAttempts = 3)

        assertFalse(target.canRetry(3))
        assertFalse(target.canRetry(4))
    }

    @Test
    fun `待ち時間が試行のたびに倍になること`() {
        val target = policy(baseDelayMillis = 500)

        // ばらつきが0のとき、待ち時間は基準の半分から始まって倍々になる
        assertEquals(250, target.delayMillisFor(1))
        assertEquals(500, target.delayMillisFor(2))
        assertEquals(1000, target.delayMillisFor(3))
    }

    @Test
    fun `待ち時間が上限を超えないこと`() {
        val target = policy(baseDelayMillis = 500, maxDelayMillis = 2000, random = maxJitterRandom)

        assertTrue(target.delayMillisFor(10) <= 2000, "上限を超えています: ${target.delayMillisFor(10)}")
    }

    @Test
    fun `ばらつきによって待ち時間が変わること`() {
        val withoutJitter = policy(baseDelayMillis = 1000, random = noJitterRandom).delayMillisFor(1)
        val withJitter = policy(baseDelayMillis = 1000, random = maxJitterRandom).delayMillisFor(1)

        // 複数の実行が同じタイミングで再試行しないよう、待ち時間にばらつきを入れる
        assertTrue(withJitter > withoutJitter, "ばらつきが反映されていません")
    }

    @Test
    fun `サーバーが指定した待機時間が優先されること`() {
        val target = policy(baseDelayMillis = 500)

        assertEquals(3000, target.delayMillisFor(1, retryAfterMillis = 3000))
    }

    @Test
    fun `サーバーが指定した待機時間も上限で切られること`() {
        val target = policy(baseDelayMillis = 500, maxDelayMillis = 8000)

        assertEquals(8000, target.delayMillisFor(1, retryAfterMillis = 60000))
    }

    @Test
    fun `混雑や一時的な障害を表すステータスは再試行対象になること`() {
        val target = policy()

        assertTrue(target.isRetryableStatus(408))
        assertTrue(target.isRetryableStatus(429))
        assertTrue(target.isRetryableStatus(500))
        assertTrue(target.isRetryableStatus(503))
    }

    @Test
    fun `何度送っても結果が変わらないステータスは再試行対象にならないこと`() {
        val target = policy()

        assertFalse(target.isRetryableStatus(400))
        assertFalse(target.isRetryableStatus(401))
        assertFalse(target.isRetryableStatus(403))
        assertFalse(target.isRetryableStatus(404))
    }

    @Test
    fun `最大試行回数が0以下の場合は生成できないこと`() {
        assertThrows(IllegalArgumentException::class.java) {
            HttpRetryPolicy(maxAttempts = 0)
        }
    }

    @Test
    fun `上限が基準より小さい場合は生成できないこと`() {
        assertThrows(IllegalArgumentException::class.java) {
            HttpRetryPolicy(maxAttempts = 3, baseDelayMillis = 1000, maxDelayMillis = 500)
        }
    }
}
