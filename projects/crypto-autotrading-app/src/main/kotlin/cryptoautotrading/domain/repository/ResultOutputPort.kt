package cryptoautotrading.domain.repository

import cryptoautotrading.domain.model.TradeAction

/**
 * 判定結果を出力するインターフェース
 */
interface ResultOutputPort {
    /**
     * 結果を出力する
     */
    fun printResult(
        price: Double,
        action: TradeAction,
        reason: String,
        profitAndLoss: Double,
        estimatedProfitAndLoss: Double
    )
}
