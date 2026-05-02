package cryptoautotrading.domain.repository

import java.math.BigDecimal

/**
 * 取引履歴を保存するインターフェース
 */
interface TradeHistoryRepository {
    /**
     * 実行結果を追記する
     */
    fun append(
        datetime: String,
        price: BigDecimal,
        sign: String,
        reason: String,
        profitAndLoss: BigDecimal,
        isHolding: Boolean,
        fee: BigDecimal
    )
}
