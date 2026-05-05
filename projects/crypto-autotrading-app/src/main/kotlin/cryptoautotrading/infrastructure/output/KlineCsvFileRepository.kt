package cryptoautotrading.infrastructure.output

import cryptoautotrading.domain.model.Kline
import cryptoautotrading.domain.repository.KlineCsvRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

/**
 * KlineCsvRepository のファイルシステム向け実装。
 * K線データをCSV形式でファイルに保存する。
 */
class KlineCsvFileRepository : KlineCsvRepository {

    private val logger = KotlinLogging.logger {}

    override fun save(klines: List<Kline>, outputPath: String) {
        logger.info { "過去K線データのCSV保存処理を開始します: $outputPath" }

        try {
            val file = File(outputPath)
            val parentDir = file.parentFile
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs()
            }

            file.bufferedWriter().use { writer ->
                // ヘッダー書き込み
                writer.write("openTime,open,high,low,close,volume\n")

                // データ書き込み
                klines.forEach { kline ->
                    writer.write("${kline.openTime},${kline.open},${kline.high},${kline.low},${kline.close},${kline.volume}\n")
                }
            }
            logger.info { "過去K線データのCSV保存が完了しました。保存件数: ${klines.size}" }
        } catch (e: Exception) {
            logger.error(e) { "過去K線データのCSV保存に失敗しました。パス: $outputPath" }
            throw e
        }
    }
}
