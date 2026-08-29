package cryptoautotrading.domain.backtest

import java.math.BigDecimal

/**
 * バックテスト全体の成績を表すモデル。
 * サマリーCSVの1行に対応する。
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
 * @property takeProfitCount 利確で売却した回数
 * @property stopLossCount 損切りで売却した回数
 * @property winRate 売却回数のうち、利確だった割合
 * @property averageProfit 利確1回あたりの平均利益
 * @property averageLoss 損切り1回あたりの平均損失
 * @property maxProfit 1回の売却で得た最大利益
 * @property maxLoss 1回の売却で出た最大損失
 * @property maxConsecutiveLossCount 連続して損切りした最大回数
 * @property hasOpenPosition バックテスト終了時点で未売却の保有が残っているか
 * @property feeRate 約定価格に織り込んだ手数料率
 * @property slippageRate 約定価格に織り込んだスリッページ率
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
    val maxDrawdown: BigDecimal,
    val takeProfitCount: Int,
    val stopLossCount: Int,
    val winRate: BigDecimal,
    val averageProfit: BigDecimal,
    val averageLoss: BigDecimal,
    val maxProfit: BigDecimal,
    val maxLoss: BigDecimal,
    val maxConsecutiveLossCount: Int,
    val hasOpenPosition: Boolean,
    val feeRate: BigDecimal = BigDecimal.ZERO,
    val slippageRate: BigDecimal = BigDecimal.ZERO
)
