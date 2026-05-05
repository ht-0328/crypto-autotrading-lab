package cryptoautotrading.infrastructure.output

import com.opencsv.bean.CsvBindByName

/**
 * opencsvを利用して過去K線CSVを読み込むためのデータ転送オブジェクト（DTO）。
 *
 * CSVファイルのヘッダー行と各データ行の各列をマッピングするために使用する。
 *
 * @property openTime K線の開始時刻
 * @property open その期間の最初の価格（始値）
 * @property high その期間の中で一番高かった価格（高値）
 * @property low その期間の中で一番安かった価格（安値）
 * @property close その期間の最後の価格（終値）
 * @property volume その期間の取引量（取引高）
 */
class KlineCsvRow {
    @CsvBindByName(column = "openTime")
    var openTime: String? = null

    @CsvBindByName(column = "open")
    var open: String? = null

    @CsvBindByName(column = "high")
    var high: String? = null

    @CsvBindByName(column = "low")
    var low: String? = null

    @CsvBindByName(column = "close")
    var close: String? = null

    @CsvBindByName(column = "volume")
    var volume: String? = null
}
