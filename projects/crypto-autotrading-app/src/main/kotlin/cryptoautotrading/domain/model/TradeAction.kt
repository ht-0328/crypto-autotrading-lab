package cryptoautotrading.domain.model

/**
 * 売買判定のアクションを表す列挙型
 */
enum class TradeAction(val description: String) {
    BUY_CANDIDATE("買い候補"),
    SELL_CANDIDATE("売り候補"),
    SKIP("見送り"),
    HOLDING("保有中")
}
