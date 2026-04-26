package cryptoautotrading.infrastructure.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ConfigLoaderPathTest {

    @Test
    fun `APP_CONFIG_PATHが指定されている場合はそのパスを優先すること`() {
        val resolved = ConfigLoader.resolveConfigPath("/tmp/custom-config.yaml")
        assertEquals("/tmp/custom-config.yaml", resolved)
    }

    @Test
    fun `APP_CONFIG_PATHが空文字の場合はデフォルトまたはフォールバックを返すこと`() {
        val resolved = ConfigLoader.resolveConfigPath("")
        // テスト実行ディレクトリによってデフォルトかフォールバックのどちらかになるため、期待値を実装と同じ式で組み立てる
        val expected = if (java.io.File("config/application-gmo.yaml").exists()) {
            "config/application-gmo.yaml"
        } else {
            "../../config/application-gmo.yaml"
        }
        assertEquals(expected, resolved)
    }
}
