package cryptoautotrading.infrastructure.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import cryptoautotrading.domain.model.AppConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

/**
 * 設定ファイルを読み込むためのオブジェクト
 */
object ConfigLoader {

    private val logger = KotlinLogging.logger {}
    private val mapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule.Builder().build())
    private const val DEFAULT_CONFIG_PATH = "config/application-gmo.yaml"
    private const val FALLBACK_CONFIG_PATH = "/workspace/config/application-gmo.yaml"

    /**
     * アプリケーション設定を読み込む
     *
     * @return 読み込んだAppConfig
     * @throws IllegalArgumentException 設定ファイルが見つからない場合
     */
    fun load(): AppConfig {
        logger.info { "ConfigLoader: 設定ファイルの読み込みを開始します" }

        val file = File(resolveConfigPath(System.getenv("APP_CONFIG_PATH")))

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

    internal fun resolveConfigPath(configPathEnv: String?): String {
        if (!configPathEnv.isNullOrBlank()) {
            return configPathEnv
        }

        if (File(DEFAULT_CONFIG_PATH).exists()) {
            return DEFAULT_CONFIG_PATH
        }

        // projects/crypto-autotrading-appから実行している場合のフォールバック
        return FALLBACK_CONFIG_PATH
    }
}
