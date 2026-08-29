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

    @Test
    fun `環境変数が未指定または空文字なら未指定として扱うこと`() {
        // GitHub Actions は未登録の Variable を空文字で渡すため、空文字は「未指定」でなければならない
        org.junit.jupiter.api.Assertions.assertNull(ConfigLoader.parseIntEnv("TRADING_TRADE_AMOUNT", null))
        org.junit.jupiter.api.Assertions.assertNull(ConfigLoader.parseIntEnv("TRADING_TRADE_AMOUNT", ""))
        org.junit.jupiter.api.Assertions.assertNull(ConfigLoader.parseIntEnv("TRADING_TRADE_AMOUNT", "   "))
        org.junit.jupiter.api.Assertions.assertNull(ConfigLoader.parseDoubleEnv("TRADING_BUY_THRESHOLD", ""))
        org.junit.jupiter.api.Assertions.assertNull(ConfigLoader.parseBooleanEnv("REAL_TRADING_ENABLED", ""))
        org.junit.jupiter.api.Assertions.assertNull(ConfigLoader.parseOrderSizingModeEnv("TRADING_ORDER_SIZING_MODE", ""))
    }

    @Test
    fun `環境変数が指定されていれば解釈した値を返すこと`() {
        assertEquals(1500, ConfigLoader.parseIntEnv("TRADING_TRADE_AMOUNT", "1500"))
        assertEquals(0.01, ConfigLoader.parseDoubleEnv("TRADING_BUY_THRESHOLD", "0.01"))
        assertEquals(true, ConfigLoader.parseBooleanEnv("REAL_TRADING_ENABLED", "true"))
        assertEquals(
            cryptoautotrading.domain.model.OrderSizingMode.ALL_IN,
            ConfigLoader.parseOrderSizingModeEnv("TRADING_ORDER_SIZING_MODE", "ALL_IN")
        )
    }

    @Test
    fun `環境変数が空でないのに解釈できない場合は起動時に例外になること`() {
        val intError = org.junit.jupiter.api.assertThrows<IllegalStateException> {
            ConfigLoader.parseIntEnv("TRADING_TRADE_AMOUNT", "abc")
        }
        assertEquals("環境変数 TRADING_TRADE_AMOUNT の値を数値として解釈できません", intError.message)

        org.junit.jupiter.api.assertThrows<IllegalStateException> {
            ConfigLoader.parseDoubleEnv("TRADING_BUY_THRESHOLD", "abc")
        }
        org.junit.jupiter.api.assertThrows<IllegalStateException> {
            ConfigLoader.parseBooleanEnv("REAL_TRADING_ENABLED", "yes")
        }
        org.junit.jupiter.api.assertThrows<IllegalStateException> {
            ConfigLoader.parseOrderSizingModeEnv("TRADING_ORDER_SIZING_MODE", "ALLIN")
        }
    }

    @Test
    fun `例外メッセージに設定値そのものを含めないこと`() {
        // 設定値が秘密情報である可能性があるため、変数名だけを出す
        val exception = org.junit.jupiter.api.assertThrows<IllegalStateException> {
            ConfigLoader.parseIntEnv("TRADING_TRADE_AMOUNT", "secret-value")
        }
        org.junit.jupiter.api.Assertions.assertFalse(exception.message!!.contains("secret-value"))
    }
}
