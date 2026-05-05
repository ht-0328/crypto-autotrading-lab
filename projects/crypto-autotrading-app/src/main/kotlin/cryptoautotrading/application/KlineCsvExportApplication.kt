package cryptoautotrading.application

import cryptoautotrading.domain.model.Kline
import cryptoautotrading.domain.repository.KlineCsvRepository
import cryptoautotrading.domain.repository.MarketDataClient
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * 過去K線CSV作成機能のユースケース（アプリケーション層）
 */
class KlineCsvExportApplication(
    private val marketDataClient: MarketDataClient,
    private val klineCsvRepository: KlineCsvRepository
) {
    private val logger = KotlinLogging.logger {}
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    /**
     * 指定期間のK線を取得し、CSVに保存する
     *
     * @param symbol 対象通貨 (例: "BTC")
     * @param interval K線間隔 (例: "5min")
     * @param startDateStr 取得開始日 (yyyyMMdd)
     * @param endDateStr 取得終了日 (yyyyMMdd)
     * @param outputPath 保存先CSVパス
     */
    suspend fun export(
        symbol: String?,
        interval: String?,
        startDateStr: String?,
        endDateStr: String?,
        outputPath: String?
    ) {
        logger.info { "過去K線CSV作成処理を開始します。開始日=$startDateStr, 終了日=$endDateStr, 出力先=$outputPath" }

        // 必須入力値チェック
        if (symbol.isNullOrBlank() || interval.isNullOrBlank() || startDateStr.isNullOrBlank() || endDateStr.isNullOrBlank() || outputPath.isNullOrBlank()) {
            throw IllegalArgumentException("必須パラメータが不足しています。symbol=$symbol, interval=$interval, startDate=$startDateStr, endDate=$endDateStr, outputPath=$outputPath")
        }

        // 日付形式と前後関係のチェック
        val startDate: LocalDate
        val endDate: LocalDate
        try {
            startDate = LocalDate.parse(startDateStr, dateFormatter)
            endDate = LocalDate.parse(endDateStr, dateFormatter)
        } catch (e: DateTimeParseException) {
            throw IllegalArgumentException("日付形式が不正です。yyyyMMdd形式で指定してください。startDate=$startDateStr, endDate=$endDateStr", e)
        }

        if (startDate.isAfter(endDate)) {
            throw IllegalArgumentException("開始日が終了日より後になっています。startDate=$startDateStr, endDate=$endDateStr")
        }

        val allKlines = mutableListOf<Kline>()

        // 各日付についてAPIを呼び出す
        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            val dateStr = currentDate.format(dateFormatter)
            logger.info { "K線データを取得中: $dateStr" }

            try {
                val response = marketDataClient.getKlines(symbol, interval, dateStr)
                if (response.data.isNotEmpty()) {
                    allKlines.addAll(response.data)
                    logger.debug { "取得件数: ${response.data.size} 件 ($dateStr)" }
                } else {
                    logger.warn { "取得件数が0件です ($dateStr)" }
                }
            } catch (e: Exception) {
                logger.error(e) { "K線データの取得に失敗しました。対象日: $dateStr" }
                throw e
            }

            currentDate = currentDate.plusDays(1)
        }

        // openTimeの昇順でソートし、重複を排除する
        val uniqueKlines = allKlines
            .sortedBy { it.openTime }
            .distinctBy { it.openTime }

        logger.info { "全取得完了。総件数（重複排除前）: ${allKlines.size}, 重複排除後: ${uniqueKlines.size}" }

        // CSVに保存
        klineCsvRepository.save(uniqueKlines, outputPath)
        logger.info { "過去K線CSV作成処理が正常に完了しました。" }
    }
}
