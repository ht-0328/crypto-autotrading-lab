package cryptoautotrading.infrastructure.output

import com.opencsv.bean.CsvBindByName

/**
 * opencsvを利用して過去K線CSVを読み込むためのデータ転送オブジェクト（DTO）。
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
