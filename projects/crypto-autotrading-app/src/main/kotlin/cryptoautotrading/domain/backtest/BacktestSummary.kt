package cryptoautotrading.domain.backtest

import java.math.BigDecimal

/**
 * バックテスト全体の成績を表すサマリー情報
 *
 * @property strategyName 使用した売買戦略名
 * @property initialCapital 開始時点の仮想資金
 * @property finalAssetValue 終了時点の総資産額
 * @property realizedProfitAndLoss 確定損益
 * @property totalReturnRate 初期資金に対する増減率
 * @property tradeCount 売買回数
 * @property buyCount 買い回数
 * @property sellCount 売り回数
 * @property maxDrawdown 最大ドローダウン
 */
data class BacktestSummary(
    val strategyName: String,
    val initialCapital: BigDecimal,
    val finalAssetValue: BigDecimal,
    val realizedProfitAndLoss: BigDecimal,
    val totalReturnRate: BigDecimal,
    val tradeCount: Int,
    val buyCount: Int,
    val sellCount: Int,
    val maxDrawdown: BigDecimal
)
