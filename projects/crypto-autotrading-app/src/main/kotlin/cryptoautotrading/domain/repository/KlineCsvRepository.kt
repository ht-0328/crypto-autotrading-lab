package cryptoautotrading.domain.repository

import cryptoautotrading.domain.model.Kline

/**
 * 取得したK線（ローソク足）データをCSVとして保存するためのリポジトリ
 */
interface KlineCsvRepository {
    /**
     * K線データのリストをCSVファイルに保存する。
     *
     * @param klines 保存するK線データのリスト
     * @param outputPath 保存先のファイルパス
     */
    fun save(klines: List<Kline>, outputPath: String)
}
