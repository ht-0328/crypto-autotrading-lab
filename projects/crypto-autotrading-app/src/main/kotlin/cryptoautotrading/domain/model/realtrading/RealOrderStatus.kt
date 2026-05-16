package cryptoautotrading.domain.model.realtrading

/**
 * リアル注文のステータスを表す列挙型
 *
 * @property WAITING 待機中
 * @property ORDERED 注文済
 * @property EXECUTED 約定済
 * @property CANCELED キャンセル済
 * @property UNCONFIRMED 未確認
 * @property FAILED 失敗
 */
enum class RealOrderStatus {
    /** 待機中 */
    WAITING,

    /** 注文済 */
    ORDERED,

    /** 約定済 */
    EXECUTED,

    /** キャンセル済 */
    CANCELED,

    /** 未確認 */
    UNCONFIRMED,

    /** 失敗 */
    FAILED
}
