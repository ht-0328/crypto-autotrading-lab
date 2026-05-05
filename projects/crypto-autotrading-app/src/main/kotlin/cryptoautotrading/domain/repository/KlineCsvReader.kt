package cryptoautotrading.domain.repository

import cryptoautotrading.domain.model.Kline

/**
 * バックテストで使用する過去K線（ローソク足）データを読み込むためのリポジトリ
 */
interface KlineCsvReader {
    /**
     * 指定されたパスからCSVファイルを読み込み、K線データのリストを返す。
     *
     * @param inputPath 読み込むCSVファイルのパス
     * @return 読み込んだK線データのリスト。openTime の昇順でソートされ、重複は排除される。
     */
    fun read(inputPath: String): List<Kline>
}
