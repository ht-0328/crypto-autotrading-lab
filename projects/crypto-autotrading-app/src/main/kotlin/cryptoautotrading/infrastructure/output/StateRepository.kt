package cryptoautotrading.infrastructure.output

import cryptoautotrading.domain.model.SimulationState
import cryptoautotrading.domain.repository.SimulationStateRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * シミュレーション状態をファイルに保存・読み込みするリポジトリ
 *
 * @property stateFilePath 状態を保存するファイルのパス
 */
class StateRepository(private val stateFilePath: String) : SimulationStateRepository {

    private val logger = KotlinLogging.logger {}
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * 状態ファイルからシミュレーション状態を読み込む。
     * ファイルが存在しない場合は初期状態を返す。
     * 読み込みに失敗した場合（ファイルが壊れている等）は例外を送出する。
     *
     * @return 読み込んだシミュレーション状態、または初期状態
     */
    override fun load(): SimulationState {
        val file = File(stateFilePath)
        return if (file.exists()) {
            try {
                val content = file.readText()
                json.decodeFromString<SimulationState>(content)
            } catch (e: Exception) {
                logger.error(e) { "状態ファイルが壊れている可能性があります。読み込みに失敗しました。パス: $stateFilePath" }
                throw e
            }
        } else {
            logger.info { "State file does not exist at $stateFilePath, returning default state." }
            SimulationState()
        }
    }

    /**
     * シミュレーション状態をファイルに保存する。
     *
     * 書き込み途中でプロセスが停止しても状態ファイルが壊れないよう、
     * 同じディレクトリの一時ファイルに書いてから原子的に置き換える。
     * 保存に失敗した場合は、呼び出し元が異常終了できるように例外を送出する。
     *
     * @param state 保存するシミュレーション状態
     */
    override fun save(state: SimulationState) {
        logger.info { "状態ファイル (state.json) の保存処理を開始します" }

        val file = File(stateFilePath)
        val tempFile = File(file.absoluteFile.parentFile, "${file.name}$TEMP_FILE_SUFFIX")

        try {
            logger.debug { "状態ファイル保存先: ${file.absolutePath}" }

            val parentDir = file.absoluteFile.parentFile
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs()
            }
            val content = json.encodeToString(state)
            tempFile.writeText(content)
            replaceAtomically(tempFile, file)
            logger.info { "状態ファイルを保存しました: $stateFilePath" }
        } catch (e: Exception) {
            // 保存内容には注文IDや残高が含まれるため、メッセージには state を含めない
            logger.error(e) { "状態ファイルの保存に失敗しました。パス: $stateFilePath" }
            throw e
        } finally {
            deleteTempFileIfExists(tempFile)
        }
    }

    /**
     * 一時ファイルで保存先ファイルを置き換える。
     *
     * 原子的な置き換えに対応していないファイルシステム（GCS のマウント等）では、
     * 通常の置き換えにフォールバックする。
     *
     * @param source 置き換え元の一時ファイル
     * @param destination 置き換え先のファイル
     */
    private fun replaceAtomically(source: File, destination: File) {
        try {
            Files.move(source.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } catch (e: AtomicMoveNotSupportedException) {
            logger.warn(e) { "原子的な置き換えに対応していないため、通常の置き換えで保存します。パス: ${destination.path}" }
            Files.move(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    /**
     * 保存に失敗した場合などに残る一時ファイルを削除する。
     *
     * 削除に失敗しても保存処理の結果には影響させない。
     *
     * @param tempFile 削除する一時ファイル
     */
    private fun deleteTempFileIfExists(tempFile: File) {
        if (!tempFile.exists()) {
            return
        }
        if (!tempFile.delete()) {
            logger.warn { "一時ファイルの削除に失敗しました。パス: ${tempFile.path}" }
        }
    }

    private companion object {
        /** 保存時に使用する一時ファイルの接尾辞 */
        const val TEMP_FILE_SUFFIX = ".tmp"
    }
}
