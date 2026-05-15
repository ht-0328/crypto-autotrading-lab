package cryptoautotrading.infrastructure.exchange.gmo.dto

import kotlinx.serialization.Serializable

/**
 * GMO API 約定情報取得APIのリクエストDTO。
 *
 * @property orderId 注文ID (orderIdまたはexecutionIdのいずれかが必須)
 * @property executionId 約定ID
 */
@Serializable
data class GmoExecutionsRequestDto(
    val orderId: Long?,
    val executionId: String?
)
