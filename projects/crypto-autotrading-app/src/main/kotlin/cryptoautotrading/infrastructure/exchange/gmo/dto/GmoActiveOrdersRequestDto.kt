package cryptoautotrading.infrastructure.exchange.gmo.dto

import kotlinx.serialization.Serializable

/**
 * GMO API 有効注文一覧取得APIのリクエストDTO。
 *
 * @property symbol 取扱銘柄
 * @property page 取得対象ページ
 * @property count 1ページ当りの取得件数
 */
@Serializable
data class GmoActiveOrdersRequestDto(
    val symbol: String,
    val page: Int?,
    val count: Int?
)
