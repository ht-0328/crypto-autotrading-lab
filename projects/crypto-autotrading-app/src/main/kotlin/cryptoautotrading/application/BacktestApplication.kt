package cryptoautotrading.application

import cryptoautotrading.domain.backtest.BacktestEngine
import cryptoautotrading.domain.model.TradingConfig
import cryptoautotrading.domain.repository.BacktestResultOutputPort
import cryptoautotrading.domain.repository.KlineCsvReader
import cryptoautotrading.domain.strategy.SafeReboundStrategy
import cryptoautotrading.domain.strategy.SimpleContrarianStrategy
import cryptoautotrading.domain.strategy.TradingStrategy
import io.github.oshai.kotlinlogging.KotlinLogging
import java.math.BigDecimal

/**
 * バックテスト機能のユースケース（アプリケーション層）
 */
class BacktestApplication(
    private val klineCsvReader: KlineCsvReader,
    private val resultOutputPort: BacktestResultOutputPort
) {
    private val logger = KotlinLogging.logger {}
    private val engine = BacktestEngine()

    /**
     * バックテストを実行する
     *
     * @param klineCsvPath 入力となる過去K線CSVのパス
     * @param strategyName 使用する戦略名
     * @param initialCapitalStr 初期資金（文字列）
     * @param summaryOutputPath サマリーの出力先パス
     * @param stepsOutputPath 明細の出力先パス
     */
    fun run(
        klineCsvPath: String?,
        strategyName: String?,
        initialCapitalStr: String?,
        summaryOutputPath: String?,
        stepsOutputPath: String?
    ) {
        logger.info { "バックテストアプリケーションを開始します" }

        // 入力チェック
        if (klineCsvPath.isNullOrBlank()) throw IllegalArgumentException("過去K線CSVファイルのパスが指定されていません")
        if (strategyName.isNullOrBlank()) throw IllegalArgumentException("使用する売買戦略名が指定されていません")
        if (initialCapitalStr.isNullOrBlank()) throw IllegalArgumentException("初期資金が指定されていません")
        if (summaryOutputPath.isNullOrBlank()) throw IllegalArgumentException("サマリー出力先パスが指定されていません")
        if (stepsOutputPath.isNullOrBlank()) throw IllegalArgumentException("明細出力先パスが指定されていません")

        val initialCapital = try {
            BigDecimal(initialCapitalStr)
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("初期資金が数値として解釈できません: $initialCapitalStr", e)
        }

        if (initialCapital <= BigDecimal.ZERO) {
            throw IllegalArgumentException("初期資金が0以下です: $initialCapital")
        }

        // CSV読み込み
        val klines = klineCsvReader.read(klineCsvPath)

        // 戦略の生成 (本来は外部から注入すべきだが要件に合わせてダミー設定を生成)
        val config = TradingConfig(
            strategyName = strategyName,
            symbol = "BTC",
            initialCapital = initialCapital.toInt(),
            tradeAmount = 1000,
            buyThreshold = 0.005,
            sellThreshold = 0.005,
            volatilityThreshold = 0.003,
            sharpChangeThreshold = 0.01
        )
        val strategy = createStrategy(config)

        // バックテスト実行
        val result = engine.run(
            klines = klines,
            strategy = strategy,
            initialCapital = initialCapital,
            tradeAmount = config.tradeAmount
        )

        // 結果出力
        resultOutputPort.output(result, summaryOutputPath, stepsOutputPath)

        logger.info { "バックテストアプリケーションが正常に完了しました" }
    }

    private fun createStrategy(config: TradingConfig): TradingStrategy {
        return when (config.strategyName) {
            "SafeReboundStrategy" -> SafeReboundStrategy(config)
            "SimpleContrarianStrategy" -> SimpleContrarianStrategy(config)
            else -> throw IllegalArgumentException("対応していない売買戦略名が指定されました: ${config.strategyName}")
        }
    }
}
