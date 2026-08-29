package cryptoautotrading.infrastructure.lock

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class ExecutionLockTest {

    private val zone: ZoneId = ZoneId.of("Asia/Tokyo")

    private fun clockAt(millis: Long): Clock = Clock.fixed(Instant.ofEpochMilli(millis), zone)

    @Test
    fun `ロックを取得できた場合は処理が実行されること`(@TempDir tempDir: Path) = runTest {
        val lockFile = tempDir.resolve("state.json.lock").toFile()
        val lock = ExecutionLock(lockFile, clockAt(1_000_000L))

        val result = lock.withLock { "実行しました" }

        assertEquals("実行しました", result)
    }

    @Test
    fun `処理の終了後にロックが解放されること`(@TempDir tempDir: Path) = runTest {
        val lockFile = tempDir.resolve("state.json.lock").toFile()
        val lock = ExecutionLock(lockFile, clockAt(1_000_000L))

        lock.withLock { "実行しました" }

        assertFalse(lockFile.exists(), "処理が終わったらロックは残らないこと")
    }

    @Test
    fun `処理が例外で終わってもロックが解放されること`(@TempDir tempDir: Path) = runTest {
        val lockFile = tempDir.resolve("state.json.lock").toFile()
        val lock = ExecutionLock(lockFile, clockAt(1_000_000L))

        runCatching {
            lock.withLock { throw IllegalStateException("失敗") }
        }

        // 解放しないと、以降の実行がすべてスキップされ損切りも動かなくなる
        assertFalse(lockFile.exists(), "例外で終わってもロックは残らないこと")
    }

    @Test
    fun `別の実行がロックを保持している場合は処理が実行されないこと`(@TempDir tempDir: Path) = runTest {
        val lockFile = tempDir.resolve("state.json.lock").toFile()
        lockFile.writeText("acquiredAt=2026-08-29T10:00:00, execution=other")
        val now = 1_000_000L
        lockFile.setLastModified(now)
        val lock = ExecutionLock(lockFile, clockAt(now))

        var executed = false
        val result = lock.withLock { executed = true }

        assertNull(result, "実行されなかったことが呼び出し元に伝わること")
        assertFalse(executed)
    }

    @Test
    fun `別の実行がロックを保持している場合でもそのロックを消さないこと`(@TempDir tempDir: Path) = runTest {
        val lockFile = tempDir.resolve("state.json.lock").toFile()
        lockFile.writeText("acquiredAt=2026-08-29T10:00:00, execution=other")
        val now = 1_000_000L
        lockFile.setLastModified(now)
        val lock = ExecutionLock(lockFile, clockAt(now))

        lock.withLock { }

        assertTrue(lockFile.exists(), "動いている実行のロックを奪ってはいけない")
    }

    @Test
    fun `古くなったロックは奪って処理が実行されること`(@TempDir tempDir: Path) = runTest {
        val lockFile = tempDir.resolve("state.json.lock").toFile()
        lockFile.writeText("acquiredAt=2026-08-29T09:00:00, execution=crashed")
        val lockedAt = 1_000_000L
        lockFile.setLastModified(lockedAt)
        // 落ちた実行のロックが残ったまま16分経過した状態
        val sixteenMinutesLater = lockedAt + 16 * 60 * 1000L
        val lock = ExecutionLock(lockFile, clockAt(sixteenMinutesLater))

        val result = lock.withLock { "実行しました" }

        assertEquals("実行しました", result, "落ちた実行のロックで永久に止まってはいけない")
    }

    @Test
    fun `古さの判定の境界より前ならロックは奪わないこと`(@TempDir tempDir: Path) = runTest {
        val lockFile = tempDir.resolve("state.json.lock").toFile()
        lockFile.writeText("acquiredAt=2026-08-29T10:00:00, execution=other")
        val lockedAt = 1_000_000L
        lockFile.setLastModified(lockedAt)
        val fourteenMinutesLater = lockedAt + 14 * 60 * 1000L
        val lock = ExecutionLock(lockFile, clockAt(fourteenMinutesLater))

        val result = lock.withLock { "実行しました" }

        assertNull(result)
    }

    @Test
    fun `ロックファイルに取得時刻が記録されること`(@TempDir tempDir: Path) = runTest {
        val lockFile = tempDir.resolve("state.json.lock").toFile()
        val lock = ExecutionLock(lockFile, clockAt(1_000_000L))

        var content = ""
        lock.withLock { content = lockFile.readText() }

        // 残ったロックの調査に使えるよう、いつ誰が取得したかを残す
        assertTrue(content.contains("acquiredAt="), "取得時刻が記録されていること: $content")
        assertTrue(content.contains("execution="), "実行の識別子が記録されていること: $content")
    }
}
