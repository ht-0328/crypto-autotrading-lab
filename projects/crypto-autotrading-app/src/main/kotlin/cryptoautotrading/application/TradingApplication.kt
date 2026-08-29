package cryptoautotrading.application

import cryptoautotrading.domain.model.AppConfig
import cryptoautotrading.domain.model.Kline
import cryptoautotrading.domain.model.TradeAction
import cryptoautotrading.domain.repository.MarketDataClient
import cryptoautotrading.domain.repository.ResultOutputPort
import cryptoautotrading.domain.model.TradingConfig
import cryptoautotrading.domain.repository.SimulationStateRepository
import cryptoautotrading.domain.repository.TradeHistoryRepository
import cryptoautotrading.domain.simulation.ProfitAndLossCalculator
import cryptoautotrading.domain.simulation.SimulationService
import cryptoautotrading.domain.strategy.CooldownReboundStrategy
import cryptoautotrading.domain.strategy.SafeReboundStrategy
import cryptoautotrading.domain.strategy.SimpleContrarianStrategy
import cryptoautotrading.domain.strategy.TrendConfirmReboundStrategy
import cryptoautotrading.domain.strategy.AtrTrendConfirmReboundStrategy
import cryptoautotrading.domain.strategy.TradingStrategy
import cryptoautotrading.domain.realtrading.RealTradingService
import cryptoautotrading.domain.realtrading.RealTradingClient
import cryptoautotrading.domain.marketdata.MarketDataValidator
import cryptoautotrading.domain.model.TradeDecision
import cryptoautotrading.domain.time.TradingTime
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * トレーディングアプリケーションのメインロジックを実行するクラス
 *
 * @property config アプリケーション設定
 * @property marketDataClient 市場データクライアント
 * @property stateRepository 状態リポジトリ
 * @property tradeHistoryRepository 取引履歴リポジトリ
 * @property resultOutputPort 結果出力ポート
 * @property realTradingExchangeClient リアル取引の取引所操作を行うクライアント
 * @property clock 時刻の取得に使う時計。テストでは固定した時刻に差し替える
 */
class TradingApplication(
    private val config: AppConfig,
    private val marketDataClient: MarketDataClient,
    private val stateRepository: SimulationStateRepository,
    private val tradeHistoryRepository: TradeHistoryRepository,
    private val resultOutputPort: ResultOutputPort,
    private val realTradingExchangeClient: RealTradingClient? = null,
    private val clock: Clock = TradingTime.systemClock()
) {

    private val logger = KotlinLogging.logger {}
    private val simulationService = SimulationService(clock)
    private val marketDataValidator = MarketDataValidator(clock)
    private val realTradingService = RealTradingService(exchangeClient = realTradingExchangeClient, clock = clock)
    private val pnlCalculator = ProfitAndLossCalculator()

/**
     * アプリケーションの実行を開始する
     */
    suspend fun run() {
        try {
            logger.info { "TradingApplication のメイン処理を開始します" }
            logger.debug { "実行設定: $config" }

            // 1. 状態の読み込みと初期資金の反映
            var currentState = stateRepository.load()

            val isUninitializedState = currentState.cashBalance.compareTo(java.math.BigDecimal.ZERO) == 0 &&
                currentState.buyPrice.compareTo(java.math.BigDecimal.ZERO) == 0 &&
                currentState.holdingAmount.compareTo(java.math.BigDecimal.ZERO) == 0 &&
                currentState.realizedProfitAndLoss.compareTo(java.math.BigDecimal.ZERO) == 0 &&
                currentState.lastUpdatedAt.isEmpty()

            val isLegacyHoldingState = currentState.cashBalance.compareTo(java.math.BigDecimal.ZERO) == 0 &&
                currentState.isHolding && currentState.holdingAmount > java.math.BigDecimal.ZERO

            if (isUninitializedState || isLegacyHoldingState) {
                val initialCapital = java.math.BigDecimal(config.trading.initialCapital)
                val newCashBalance = if (currentState.isHolding) {
                    val cost = currentState.buyPrice * currentState.holdingAmount
                    val balance = initialCapital - cost
                    if (balance < java.math.BigDecimal.ZERO) java.math.BigDecimal.ZERO else balance
                } else {
                    initialCapital
                }
                currentState = currentState.copy(cashBalance = newCashBalance)
                logger.info { "初期資金を反映し、残金を $newCashBalance に設定しました" }
            }

            logger.info { "現在のシミュレーション状態を読み込みました" }
            logger.debug { "読み込んだ状態: $currentState" }

            // 2. APIからデータの取得
            val tickerPrice = fetchTickerPrice()
            val klineData = fetchKlineData()

            // データが空の場合は価格も決められないため、ここで終了する
            if (klineData.isEmpty()) {
                logger.warn { "Klines data is empty. Skipping this run." }
                return
            }

            // 3. 売買判定
            // 壊れたデータや古いデータで判定すると誤った売買サインが出る。
            // 実注文ではそれがそのまま誤発注になるため、判定の前にデータ自体を確認する。
            val validationResult = marketDataValidator.validate(klineData, config.app.interval)
            val decision = if (validationResult.isValid) {
                createStrategy(config.trading).judge(klineData, currentState)
            } else {
                logger.warn { "市場データが信用できないため、売買判定を見送ります。理由: ${validationResult.reason}" }
                TradeDecision(TradeAction.SKIP, "市場データの検証に失敗: ${validationResult.reason}")
            }
            logger.info { "Trade Decision: ${decision.action.description}, Reason: ${decision.reason}" }

            // 4. 状態の更新
            val latestKline = klineData.sortedBy { it.openTime }.last()
            val currentPrice = latestKline.close.toBigDecimal()

            val isRealTradeActive = config.realTrading.realTradeEnabled && !config.realTrading.dryRun

            // リアル取引の処理 (実注文・状態保存)
            currentState = realTradingService.executeOrderIfNeeded(
                decision = decision,
                config = config.realTrading,
                tradeAmount = config.trading.tradeAmount,
                symbol = config.trading.symbol,
                currentState = currentState,
                klineClosePrice = currentPrice,
                tickerPrice = tickerPrice,
                orderSizingMode = config.trading.orderSizingMode
            )

            // 実取引では、注文IDを失うと再発注につながるため、他の出力より先に状態を保存する
            if (isRealTradeActive) {
                stateRepository.save(currentState)
                logger.info { "実取引モードのため、注文処理直後の状態を保存しました" }
            }

            // 損益と想定損益の計算
            val pnl = pnlCalculator.calculate(
                isHolding = currentState.isHolding,
                currentPrice = currentPrice,
                buyPrice = currentState.buyPrice,
                holdingAmount = currentState.holdingAmount,
                shouldSell = decision.action == TradeAction.SELL_CANDIDATE
            )
            val fee = java.math.BigDecimal.ZERO // 手数料は現時点ではゼロとして扱う

            // 実取引モードでは、取引所側で約定を確認できたもの以外で保有状態を動かさない。
            // 売りも対象に含める。注文の受付と約定は別のため、仮想売却して保有なしにすると、
            // 取引所には残っているのに state が未保有になり、
            // 以降の損切り・保有上限の判断がすべて狂う。
            // 約定の反映は次回以降の実行で RealTradingService が注文照会を経て行う。
            val shouldBypassSimulationStateUpdate = isRealTradeActive &&
                (decision.action == TradeAction.BUY_CANDIDATE || decision.action == TradeAction.SELL_CANDIDATE)

            val nextState = if (shouldBypassSimulationStateUpdate) {
                // 注文受付と約定は別のため、約定確認するまでは保有状態を変更しない
                logger.info {
                    "実取引モードで${decision.action.description}を扱うため、" +
                        "シミュレーションによる即時の保有状態更新をバイパスします"
                }
                if (decision.action == TradeAction.SELL_CANDIDATE) {
                    logger.info { "実取引モードの売り注文は、約定を確認できるまで保有状態を維持します。" }
                }
                currentState
            } else {
                simulationService.updateState(
                    currentState = currentState,
                    decision = decision,
                    currentPrice = currentPrice,
                    tradeAmount = config.trading.tradeAmount,
                    eventTime = latestKline.openTime,
                    orderSizingMode = config.trading.orderSizingMode
                )
            }
            logger.info { "Next Simulation State: $nextState" }

            val estimatedHoldingValue = nextState.holdingAmount * currentPrice
            val totalAssetValue = nextState.cashBalance + estimatedHoldingValue

            // 5. 出力
            // コンソール出力
            resultOutputPort.printResult(
                price = currentPrice,
                action = decision.action,
                reason = decision.reason,
                profitAndLoss = pnl.profitAndLoss,
                estimatedProfitAndLoss = pnl.estimatedProfitAndLoss,
                cashBalance = nextState.cashBalance,
                holdingAmount = nextState.holdingAmount,
                buyPrice = nextState.buyPrice,
                realizedProfitAndLoss = nextState.realizedProfitAndLoss,
                estimatedHoldingValue = estimatedHoldingValue,
                totalAssetValue = totalAssetValue
            )

            // CSV出力
            val nowStr = LocalDateTime.now(clock).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            tradeHistoryRepository.append(
                datetime = nowStr,
                price = currentPrice,
                sign = decision.action.description,
                reason = decision.reason,
                profitAndLoss = pnl.profitAndLoss,
                isHolding = nextState.isHolding,
                fee = fee,
                cashBalance = nextState.cashBalance,
                holdingAmount = nextState.holdingAmount,
                buyPrice = nextState.buyPrice,
                realizedProfitAndLoss = nextState.realizedProfitAndLoss,
                estimatedHoldingValue = estimatedHoldingValue,
                totalAssetValue = totalAssetValue
            )

            // シミュレーション結果サマリーのログ出力
            logSimulationSummary(
                currentState = currentState,
                nextState = nextState,
                decision = decision,
                currentPrice = currentPrice,
                pnl = pnl,
                estimatedHoldingValue = estimatedHoldingValue,
                totalAssetValue = totalAssetValue,
                tradeAmount = config.trading.tradeAmount
            )

            // 6. 状態の保存
            stateRepository.save(nextState)

            logger.info { "TradingApplication のメイン処理が正常に完了しました" }

        } catch (e: Exception) {
            logger.error(e) { "TradingApplication の実行中にエラーが発生しました: ${e.message}" }
            throw e
        }
    }

/**
     * 取引所のAPIから最新のティッカー情報とK線（ローソク足）データを取得する。
     *
     * @return 取得したK線データのリスト
     */
    private suspend fun fetchKlineData(): List<Kline> {
        // 取引所の営業日は朝6時に切り替わる。当日分だけを取得すると、6時を過ぎた直後は
        // 判定に必要な本数が揃わず、保有中でも利確・損切りが働かない時間帯ができる。
        // 前営業日分とあわせて取得し、境界をまたいで連続した系列にする。
        val currentDate = resolveKlineTargetDate()
        val previousDate = resolvePreviousKlineTargetDate()

        val previousKlines = fetchKlinesForDate(previousDate, isRequired = false)
        val currentKlines = fetchKlinesForDate(currentDate, isRequired = true)

        val mergedKlines = (previousKlines + currentKlines)
            .distinctBy { it.openTime }
            .sortedBy { it.openTime }
            .takeLast(KLINE_WINDOW_SIZE)

        logger.debug {
            "取得したK線データ件数: 前営業日=${previousKlines.size} 件, 当営業日=${currentKlines.size} 件, " +
                "判定に使う直近=${mergedKlines.size} 件"
        }
        return mergedKlines
    }

    /**
     * 指定した営業日のK線データを取得する。
     *
     * @param date 取得する営業日（形式: yyyyMMdd）
     * @param isRequired 取得できないときに例外にするかどうか。
     *   前営業日分は補助的なので、取得できなくても当日分だけで判定を続ける
     * @return 取得したK線データのリスト
     */
    private suspend fun fetchKlinesForDate(date: String, isRequired: Boolean): List<Kline> {
        return try {
            marketDataClient.getKlines(config.trading.symbol, config.app.interval, date).data
        } catch (e: Exception) {
            if (isRequired) {
                throw e
            }
            logger.warn(e) { "前営業日($date)のK線データを取得できませんでした。当営業日分だけで判定します。" }
            emptyList()
        }
    }

    /**
     * 前営業日の日付を決定する。
     *
     * @return 対象日付の文字列（形式: yyyyMMdd）
     */
    private fun resolvePreviousKlineTargetDate(): String {
        val nowJst = ZonedDateTime.now(clock)
        val date = if (nowJst.hour < BUSINESS_DAY_START_HOUR) {
            nowJst.minusDays(2)
        } else {
            nowJst.minusDays(1)
        }
        return date.format(DateTimeFormatter.ofPattern(KLINE_DATE_PATTERN))
    }

/**
     * 取引所から最新価格（ティッカーの最終取引価格）を取得する。
     *
     * 実注文の数量は「注文金額 ÷ 価格」で決まるため、K線の終値ではなくこの価格を使う。
     * 取得や解釈に失敗した場合は、注文を見送れるように null を返す。
     *
     * @return 取引所の最新価格。取得できない場合は null
     */
    private suspend fun fetchTickerPrice(): java.math.BigDecimal? {
        return try {
            val tickerResponse = marketDataClient.getTicker(config.trading.symbol)
            val ticker = tickerResponse.data.firstOrNull()
            logger.debug {
                "取得したティッカー主要値: symbol=${ticker?.symbol}, last=${ticker?.last}, " +
                    "bid=${ticker?.bid}, ask=${ticker?.ask}"
            }
            ticker?.last?.toBigDecimalOrNull()
        } catch (e: Exception) {
            logger.warn(e) { "ティッカーの取得に失敗しました。実注文は見送られます。" }
            null
        }
    }

/**
     * K線データを取得するための対象日付を決定する。
     * GMOコイン等の取引所の仕様（営業日は朝6時切り替え）を考慮し、
     * 午前6時前の場合は前日の日付を返す。
     *
     * @return 対象日付の文字列（形式: yyyyMMdd）
     */
    private fun resolveKlineTargetDate(): String {
        val nowJst = ZonedDateTime.now(clock)
        val date = if (nowJst.hour < BUSINESS_DAY_START_HOUR) {
            nowJst.minusDays(1)
        } else {
            nowJst
        }
        return date.format(DateTimeFormatter.ofPattern(KLINE_DATE_PATTERN))
    }

/**
     * 設定値に指定されたStrategy名から、実際に使用する売買戦略を生成する。
     *
     * 未対応のStrategy名が指定された場合は、誤った戦略で実行されないように例外を投げる。
     *
     * @param config 取引関連の設定
     * @return 使用する売買戦略
     */
    private fun createStrategy(config: TradingConfig): TradingStrategy {
        return when (config.strategyName) {
            "SafeReboundStrategy" -> SafeReboundStrategy(config)
            "CooldownReboundStrategy" -> CooldownReboundStrategy(config)
            "TrendConfirmReboundStrategy" -> TrendConfirmReboundStrategy(config)
            "AtrTrendConfirmReboundStrategy" -> AtrTrendConfirmReboundStrategy(config)
            "SimpleContrarianStrategy" -> SimpleContrarianStrategy(config)
            else -> error("Unknown strategyName: ${config.strategyName}. Supported strategies: SafeReboundStrategy, CooldownReboundStrategy, TrendConfirmReboundStrategy, AtrTrendConfirmReboundStrategy, SimpleContrarianStrategy")
        }
    }

    /**
     * シミュレーションのサマリーをログ出力する
     * @param state シミュレーション状態
     * @param currentPrice 現在の価格
     */
    private fun logSimulationSummary(
        currentState: cryptoautotrading.domain.model.SimulationState,
        nextState: cryptoautotrading.domain.model.SimulationState,
        decision: cryptoautotrading.domain.model.TradeDecision,
        currentPrice: java.math.BigDecimal,
        pnl: cryptoautotrading.domain.simulation.ProfitAndLossResult,
        estimatedHoldingValue: java.math.BigDecimal,
        totalAssetValue: java.math.BigDecimal,
        tradeAmount: Int
    ) {
        val isHoldingStr = if (nextState.isHolding) "保有中" else "なし"
        val sb = java.lang.StringBuilder()
        sb.appendLine()
        sb.appendLine("============================================================")
        sb.appendLine("【シミュレーション結果サマリー】")
        sb.appendLine("------------------------------------------------------------")
        sb.appendLine("判定              : ${decision.action.description}")
        sb.appendLine("理由              : ${decision.reason}")
        sb.appendLine("現在価格          : $currentPrice")
        sb.appendLine("残金              : ${nextState.cashBalance}")
        sb.appendLine("保有BTC数量        : ${nextState.holdingAmount}")
        sb.appendLine("保有BTC評価額      : $estimatedHoldingValue")
        sb.appendLine("総資産            : $totalAssetValue")
        sb.appendLine("買値              : ${nextState.buyPrice}")
        sb.appendLine("確定損益          : ${nextState.realizedProfitAndLoss}")
        sb.appendLine("想定損益          : ${pnl.estimatedProfitAndLoss}")
        sb.appendLine("更新後の保有状態    : $isHoldingStr")

        // 買い・売りの詳細
        if (decision.action == TradeAction.BUY_CANDIDATE && !currentState.isHolding && nextState.isHolding) {
            sb.appendLine("購入金額          : $tradeAmount")
            sb.appendLine("購入BTC数量       : ${nextState.holdingAmount}")
            sb.appendLine("購入後残金        : ${nextState.cashBalance}")
        } else if (decision.action == TradeAction.SELL_CANDIDATE && currentState.isHolding && !nextState.isHolding) {
            val sellAmount = currentState.holdingAmount * currentPrice
            sb.appendLine("売却金額          : $sellAmount")
            sb.appendLine("売却BTC数量       : ${currentState.holdingAmount}")
            sb.appendLine("売却損益          : ${pnl.profitAndLoss}")
            sb.appendLine("売却後残金        : ${nextState.cashBalance}")
        }
        sb.append("============================================================")
        logger.info { sb.toString() }
    }

    private companion object {
        /** 取引所の営業日が切り替わる時刻（日本時間） */
        const val BUSINESS_DAY_START_HOUR = 6

        /** K線データの取得対象日を表す形式 */
        const val KLINE_DATE_PATTERN = "yyyyMMdd"

        /**
         * 判定に使うK線の本数。
         * 最も多く必要とする Strategy でも15本程度なので、余裕をみてこの本数に絞る。
         * 1日分すべてを検証対象にすると、判定に使わない古い箇所の欠損で見送りになってしまう。
         */
        const val KLINE_WINDOW_SIZE = 60
    }
}
