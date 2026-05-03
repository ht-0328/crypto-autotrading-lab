package cryptoautotrading.domain.repository

import java.math.BigDecimal

/**
 * 取引履歴を保存するインターフェース
 */
interface TradeHistoryRepository {
    /**
     * 実行結果を履歴として追記する。
     *
     * @param datetime 取引が実行された日時（文字列）
     * @param price 取引時の価格
     * @param sign 売買のアクション（買い、売り、見送りなど）を示す文字列
     * @param reason そのアクションを決定した理由
     * @param profitAndLoss 確定した損益
     * @param isHolding 取引後の保有状態（保有中ならtrue）
     * @param fee 取引にかかった手数料
     */
    fun append(
        datetime: String,
        price: BigDecimal,
        sign: String,
        reason: String,
        profitAndLoss: BigDecimal,
        isHolding: Boolean,
        fee: BigDecimal,
        cashBalance: BigDecimal,
        holdingAmount: BigDecimal,
        buyPrice: BigDecimal,
        realizedProfitAndLoss: BigDecimal
    )
}
