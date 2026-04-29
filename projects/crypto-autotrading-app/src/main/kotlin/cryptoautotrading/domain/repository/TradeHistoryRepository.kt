package cryptoautotrading.domain.repository

/**
 * 取引履歴を保存するインターフェース
 */
interface TradeHistoryRepository {
    /**
     * 実行結果を追記する
     */
    fun append(
        datetime: String,
        price: Double,
        sign: String,
        reason: String,
        profitAndLoss: Double,
        isHolding: Boolean,
        fee: Double
    )
}
