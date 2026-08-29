package cryptoautotrading.infrastructure.lock

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.time.Clock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 同じ状態ファイルを複数の実行が同時に読み書きしないようにするロック。
 *
 * 定期実行が重複して起動すると、2つの実行が同じ状態を「保有なし」と読み、
 * どちらも注文を出しうる。実注文では、これがそのまま二重注文になる。
 *
 * ロックはファイルの排他作成で取る。すでにファイルがあれば、別の実行が動いている
 * とみなして今回はスキップする。
 *
 * **前提**: ファイルの排他作成が原子的であることに依存する。通常のファイルシステムでは
 * 成り立つが、GCS のマウントのようにオブジェクトストレージを模したファイルシステムでは
 * 保証されないことがある。完全な排他が必要な場合は、GCS の世代条件付き書き込みなど
 * ストレージ側の仕組みを使う必要がある。
 *
 * @property lockFile ロックとして使うファイル
 * @property clock 取得時刻と古さの判定に使う時計
 * @property staleAfterMinutes この分数を過ぎたロックは、落ちた実行の置き土産とみなして奪う
 */
class ExecutionLock(
    private val lockFile: File,
    private val clock: Clock,
    private val staleAfterMinutes: Long = DEFAULT_STALE_AFTER_MINUTES
) {

    private val logger = KotlinLogging.logger {}

    /**
     * ロックを取得し、取得できた場合だけ処理を実行する。
     *
     * 取得できなかった場合は処理を実行せず null を返す。これは異常ではなく、
     * 別の実行が動いているだけなので、呼び出し元は正常終了してよい。
     *
     * @param block ロックを取得できた場合に実行する処理
     * @return 処理の戻り値。ロックを取得できなかった場合は null
     */
    suspend fun <T> withLock(block: suspend () -> T): T? {
        if (!tryAcquire()) {
            return null
        }

        return try {
            block()
        } finally {
            release()
        }
    }

    /**
     * ロックの取得を試みる。
     *
     * @return 取得できた場合は true
     */
    private fun tryAcquire(): Boolean {
        takeOverIfStale()

        return try {
            Files.createFile(lockFile.toPath())
            lockFile.writeText(buildLockContent())
            logger.debug { "実行ロックを取得しました。パス: ${lockFile.path}" }
            true
        } catch (e: FileAlreadyExistsException) {
            logger.warn(e) {
                "別の実行がロックを保持しているため、今回の実行をスキップします。パス: ${lockFile.path}"
            }
            false
        }
    }

    /**
     * 古くなったロックを削除する。
     *
     * 実行が異常終了するとロックが残る。放置すると、以降の実行がすべてスキップされ、
     * 保有しているポジションの損切りも動かなくなる。
     */
    private fun takeOverIfStale() {
        if (!lockFile.exists()) {
            return
        }

        val ageMinutes = (clock.millis() - lockFile.lastModified()) / MILLIS_PER_MINUTE
        if (ageMinutes < staleAfterMinutes) {
            return
        }

        logger.warn {
            "${ageMinutes}分前の古いロックが残っていました。落ちた実行の置き土産とみなして削除します。" +
                "パス: ${lockFile.path}"
        }
        if (!lockFile.delete()) {
            logger.warn { "古いロックの削除に失敗しました。パス: ${lockFile.path}" }
        }
    }

    /**
     * ロックを解放する。
     *
     * 解放に失敗しても、次の実行が古さの判定で奪えるため、処理は継続する。
     */
    private fun release() {
        if (!lockFile.delete()) {
            logger.warn { "実行ロックの解放に失敗しました。パス: ${lockFile.path}" }
            return
        }
        logger.debug { "実行ロックを解放しました。パス: ${lockFile.path}" }
    }

    /**
     * ロックファイルに書く内容を組み立てる。
     *
     * どの実行がいつ取得したかが分かれば、残ったロックの調査に使える。
     *
     * @return ロックファイルの内容
     */
    private fun buildLockContent(): String {
        val acquiredAt = LocalDateTime.now(clock).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val executionId = System.getenv(CLOUD_RUN_EXECUTION_ENV) ?: "unknown"
        return "acquiredAt=$acquiredAt, execution=$executionId"
    }

    private companion object {
        /** ロックが古いと判断するまでの分数。5分間隔の実行で3回分 */
        const val DEFAULT_STALE_AFTER_MINUTES = 15L

        /** 1分のミリ秒 */
        const val MILLIS_PER_MINUTE = 60_000L

        /** Cloud Run が実行ごとに設定する環境変数 */
        const val CLOUD_RUN_EXECUTION_ENV = "CLOUD_RUN_EXECUTION"
    }
}
