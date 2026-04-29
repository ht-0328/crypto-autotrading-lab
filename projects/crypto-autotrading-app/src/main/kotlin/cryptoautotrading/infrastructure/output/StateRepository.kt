package cryptoautotrading.infrastructure.output

import cryptoautotrading.domain.model.SimulationState
import cryptoautotrading.domain.repository.SimulationStateRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

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
     * ファイルが存在しない、または読み込みに失敗した場合は初期状態を返す。
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
                logger.error(e) { "Failed to load state from $stateFilePath, returning default state." }
                SimulationState()
            }
        } else {
            logger.info { "State file does not exist at $stateFilePath, returning default state." }
            SimulationState()
        }
    }

    /**
     * シミュレーション状態をファイルに保存する。
     *
     * @param state 保存するシミュレーション状態
     */
    override fun save(state: SimulationState) {
        logger.info { "状態ファイル (state.json) の保存処理を開始します" }
        try {
            val file = File(stateFilePath)

            logger.debug { "状態ファイル保存先: ${file.absolutePath}" }

            val parentDir = file.parentFile
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs()
            }
            val content = json.encodeToString(state)
            file.writeText(content)
            logger.info { "状態ファイルを保存しました: $stateFilePath" }
        } catch (e: Exception) {
            logger.error(e) { "状態ファイルの保存に失敗しました。パス: $stateFilePath, 保存しようとした状態: $state" }
        }
    }
}
