package cryptoautotrading.infrastructure.exchange.gmo.dto

import kotlinx.serialization.Serializable

/**
 * GMO API 約定情報取得APIのdata項目DTO。
 *
 * @property list 約定情報のリスト
 */
@Serializable
data class GmoExecutionsDataDto(
    val list: List<GmoExecutionDto>
)
