package cryptoautotrading.infrastructure.exchange.gmo.dto

import kotlinx.serialization.Serializable

/**
 * GMO API 注文情報取得APIのdata項目DTO。
 *
 * @property list 注文情報のリスト
 */
@Serializable
data class GmoOrdersDataDto(
    val list: List<GmoOrderDto>
)
