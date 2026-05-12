package cryptoautotrading.infrastructure.exchange.gmo.model

import kotlinx.serialization.Serializable

/**
 * GMOコインの注文IDのみを含むデータ
 *
 * @property orderId 注文ID
 */
@Serializable
data class GmoOrderIdData(
    val orderId: String
)
