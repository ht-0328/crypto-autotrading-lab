package cryptoautotrading.domain.model.realtrading

/**
 * リアル注文の方向を表す列挙型
 *
 * @property BUY 買い
 * @property SELL 売り
 */
enum class RealOrderSide {
    /** 買い */
    BUY,

    /** 売り */
    SELL
}
