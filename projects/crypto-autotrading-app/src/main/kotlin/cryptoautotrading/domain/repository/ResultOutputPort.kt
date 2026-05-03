package cryptoautotrading.domain.repository

import cryptoautotrading.domain.model.TradeAction
import java.math.BigDecimal

/**
 * 判定結果を出力するインターフェース
 */
interface ResultOutputPort {
    /**
     * 結果を出力する
     */
    fun printResult(
        price: BigDecimal,
        action: TradeAction,
        reason: String,
        profitAndLoss: BigDecimal,
        estimatedProfitAndLoss: BigDecimal,
        cashBalance: BigDecimal,
        holdingAmount: BigDecimal,
        buyPrice: BigDecimal,
        realizedProfitAndLoss: BigDecimal
    )
}
