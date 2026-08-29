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

    @Test
    fun `APP_PHASEが未指定なら設定ファイルのフェーズを使うこと`() {
        assertEquals(1, ConfigLoader.resolvePhase(null, 1))
        assertEquals(3, ConfigLoader.resolvePhase("", 3))
    }

    @Test
    fun `APP_PHASEが指定されていればその値を使うこと`() {
        assertEquals(3, ConfigLoader.resolvePhase("3", 1))
    }

    @Test
    fun `APP_PHASEが数値でない場合は起動時に例外になること`() {
        // 安全上の設定のため、黙って既定値に戻さず失敗させる
        val exception = org.junit.jupiter.api.assertThrows<IllegalStateException> {
            ConfigLoader.resolvePhase("phase1", 1)
        }
        assertEquals("環境変数 APP_PHASE の値を数値として解釈できません", exception.message)
    }
}
