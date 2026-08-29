package cryptoautotrading.domain.marketdata

/**
 * 市場データの妥当性検証の結果を表すデータクラス
 *
 * @property isValid 売買判定に使ってよいデータかどうか
 * @property reason 使えない場合の理由
 */
data class MarketDataValidationResult(
    val isValid: Boolean,
    val reason: String? = null
)
