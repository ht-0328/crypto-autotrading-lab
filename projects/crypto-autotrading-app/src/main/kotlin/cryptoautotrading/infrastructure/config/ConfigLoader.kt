package cryptoautotrading.infrastructure.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import cryptoautotrading.domain.model.AppConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.nio.file.Paths

/**
 * 設定ファイルを読み込むためのオブジェクト
 */
object ConfigLoader {

    private val logger = KotlinLogging.logger {}
    private val mapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule.Builder().build())

    /**
     * アプリケーション設定を読み込む
     *
     * @return 読み込んだAppConfig
     * @throws IllegalArgumentException 設定ファイルが見つからない場合
     */
    fun load(): AppConfig {
        logger.info { "ConfigLoader: 設定ファイルの読み込みを開始します" }

        val configPathEnv = System.getenv("APP_CONFIG_PATH")
        val configPath = if (!configPathEnv.isNullOrBlank()) {
            configPathEnv
        } else {
            // カレントディレクトリまたはプロジェクトルートから設定ファイルを探す
            val defaultPath = "config/application-gmo.yaml"
            if (File(defaultPath).exists()) {
                defaultPath
            } else {
                // projects/crypto-autotrading-appから実行している場合、絶対パスの/workspace/config/application-gmo.yamlにフォールバックする
                "/workspace/config/application-gmo.yaml"
            }
        }

        val file = File(configPath)

        logger.debug { "ConfigLoader: 実際に読み込むファイルの絶対パス = ${file.absolutePath}" }

        if (!file.exists()) {
            val errorMsg = "設定ファイルが見つかりません: ${file.absolutePath}"
            logger.error { errorMsg }
            throw IllegalArgumentException(errorMsg)
        }

        val config = mapper.readValue(file, AppConfig::class.java)
        logger.info { "ConfigLoader: 設定ファイルの読み込みが完了しました" }
        return config
    }
}
