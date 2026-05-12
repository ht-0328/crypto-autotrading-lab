package cryptoautotrading.infrastructure.exchange.gmo.model

import kotlinx.serialization.Serializable

/**
 * ページネーション情報
 *
 * @property currentPage 現在のページ
 * @property count ページあたりの件数
 */
@Serializable
data class GmoPagination(
    val currentPage: Int,
    val count: Int
)
