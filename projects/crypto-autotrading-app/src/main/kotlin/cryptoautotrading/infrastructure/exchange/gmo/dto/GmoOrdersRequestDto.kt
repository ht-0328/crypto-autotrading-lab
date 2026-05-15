package cryptoautotrading.infrastructure.exchange.gmo.dto

import kotlinx.serialization.Serializable

/**
 * GMO API 注文情報取得APIのリクエストDTO。
 *
 * @property orderId 注文ID (カンマ区切りで複数指定可能)
 */
@Serializable
data class GmoOrdersRequestDto(
    val orderId: String
)
