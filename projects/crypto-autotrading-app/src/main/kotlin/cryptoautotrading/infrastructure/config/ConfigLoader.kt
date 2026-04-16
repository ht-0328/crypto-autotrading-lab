package cryptoautotrading.infrastructure.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import cryptoautotrading.domain.model.AppConfig
import java.io.File
import java.nio.file.Paths

object ConfigLoader {

    private val mapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule.Builder().build())

    fun load(): AppConfig {
        val configPathEnv = System.getenv("APP_CONFIG_PATH")
        val configPath = if (!configPathEnv.isNullOrBlank()) {
            configPathEnv
        } else {
            // Find config file starting from current dir or project root
            val defaultPath = "config/application.yaml"
            if (File(defaultPath).exists()) {
                defaultPath
            } else {
                // If running from projects/crypto-autotrading-app, fallback to repo root config
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
