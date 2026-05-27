package cryptoautotrading.domain.model

/**
 * 注文数量モード
 */
enum class OrderSizingMode {
    /**
     * 固定金額購入（trade_amountを使用）
     */
    FIXED_AMOUNT,

    /**
     * 利用可能な残高を全て使用して購入
     */
    ALL_IN
}
