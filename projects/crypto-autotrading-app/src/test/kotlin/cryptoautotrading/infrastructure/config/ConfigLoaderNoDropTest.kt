package cryptoautotrading.infrastructure.config

import cryptoautotrading.domain.model.ApiConfig
import cryptoautotrading.domain.model.AppConfig
import cryptoautotrading.domain.model.AppSettings
import cryptoautotrading.domain.model.OrderSizingMode
import cryptoautotrading.domain.model.OutputConfig
import cryptoautotrading.domain.model.TradingConfig
import cryptoautotrading.domain.model.notification.NotificationConfig
import cryptoautotrading.domain.model.realtrading.RealTradingConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * 設定ファイルから読んだ値が、環境変数の上書き処理で落ちていないことを検査する。
 *
 * `overrideWithEnvVars` は `AppConfig` を組み立て直すため、新しい設定項目を追加したときに
 * そこへ書き足すのを忘れると、設定ファイルに書いた値が黙って捨てられる。
 * 実際にこの取りこぼしが起きたことがあるため、項目ごとではなく全体の一致で検査する。
 */
class ConfigLoaderNoDropTest {

    /** すべての項目に既定値と違う値を入れた設定 */
    private fun configWithAllFieldsSet(): AppConfig = AppConfig(
        app = AppSettings(interval = "1hour", phase = 1),
        trading = TradingConfig(
            strategyName = "CooldownReboundStrategy",
            symbol = "ETH",
            initialCapital = 12345,
            tradeAmount = 678,
            buyThreshold = 0.011,
            sellThreshold = 0.012,
            volatilityThreshold = 0.013,
            sharpChangeThreshold = 0.014,
            cooldownLength = 7,
            atrLength = 9,
            atrProfitMultiplier = 1.5,
            atrLossMultiplier = 2.5,
            orderSizingMode = OrderSizingMode.ALL_IN
        ),
        api = ApiConfig(
            retryCount = 5,
            publicBaseUrl = "https://example.com/public",
            privateBaseUrl = "https://example.com/private"
        ),
        output = OutputConfig(outputPath = "custom-trades.csv", statePath = "custom-state.json"),
        realTrading = RealTradingConfig(
            dryRun = false,
            realTradeEnabled = true,
            stopOnUnconfirmedOrder = true,
            maxOrderJpy = 11111,
            maxDailyOrderJpy = 22222,
            maxPositionJpy = 33333,
            minOrderSize = BigDecimal("0.00002"),
            sizeStep = BigDecimal("0.00003"),
            takerFeeRate = BigDecimal("0.0007"),
            maxSlippageRate = BigDecimal("0.008"),
            maxDailyLossJpy = 4444,
            maxConsecutiveLosses = 6
        ),
        notification = NotificationConfig(enabled = true, payloadKey = "text")
    )

    private fun overrideWithEnvVars(config: AppConfig): AppConfig {
        val method = ConfigLoader::class.java.getDeclaredMethod("overrideWithEnvVars", AppConfig::class.java)
        method.isAccessible = true
        return method.invoke(ConfigLoader, config) as AppConfig
    }

    @Test
    fun `環境変数を指定しなければ設定ファイルの値がすべてそのまま残ること`() {
        val base = configWithAllFieldsSet()

        val result = overrideWithEnvVars(base)

        // 項目ごとに比べるとチェック漏れが起きるため、設定全体の一致で検査する
        assertEquals(base, result, "環境変数の上書きで設定が落ちています")
    }

    @Test
    fun `リアル取引の設定がすべてそのまま残ること`() {
        val base = configWithAllFieldsSet()

        val result = overrideWithEnvVars(base)

        assertEquals(base.realTrading, result.realTrading)
    }

    @Test
    fun `通知の設定がそのまま残ること`() {
        val base = configWithAllFieldsSet()

        val result = overrideWithEnvVars(base)

        assertEquals(base.notification, result.notification)
    }
}
