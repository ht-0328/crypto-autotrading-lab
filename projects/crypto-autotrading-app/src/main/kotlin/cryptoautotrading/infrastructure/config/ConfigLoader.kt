package cryptoautotrading.infrastructure.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import cryptoautotrading.domain.model.AppConfig
import java.io.File
import java.nio.file.Paths

/**
 * 設定ファイルを読み込むためのオブジェクト
 */
object ConfigLoader {

    private val mapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule.Builder().build())

    /**
     * アプリケーション設定を読み込む
     *
     * @return 読み込んだAppConfig
     * @throws IllegalArgumentException 設定ファイルが見つからない場合
     */
    fun load(): AppConfig {
        val configPathEnv = System.getenv("APP_CONFIG_PATH")
        val configPath = if (!configPathEnv.isNullOrBlank()) {
            configPathEnv
        } else {
            // カレントディレクトリまたはプロジェクトルートから設定ファイルを探す
            val defaultPath = "config/application.yaml"
            if (File(defaultPath).exists()) {
                defaultPath
            } else {
                // projects/crypto-autotrading-appから実行している場合、リポジトリルートの設定にフォールバックする
                "../../config/application.yaml"
            }
        }

        val file = File(configPath)
        if (!file.exists()) {
            throw IllegalArgumentException("Configuration file not found at: ${file.absolutePath}")
        }

        return mapper.readValue(file, AppConfig::class.java)
    }
}
