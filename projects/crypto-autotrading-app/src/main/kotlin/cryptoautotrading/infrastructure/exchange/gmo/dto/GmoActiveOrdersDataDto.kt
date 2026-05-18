package cryptoautotrading.infrastructure.exchange.gmo.dto

import kotlinx.serialization.Serializable

/**
 * GMO API 有効注文一覧取得APIのdata項目DTO。
 *
 * @property list 有効注文のリスト
 */
@Serializable
data class GmoActiveOrdersDataDto(
    val list: List<GmoActiveOrderDto>? = null
)
