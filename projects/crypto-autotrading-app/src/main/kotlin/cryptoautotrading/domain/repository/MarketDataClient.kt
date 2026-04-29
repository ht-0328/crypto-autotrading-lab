package cryptoautotrading.domain.repository

import cryptoautotrading.domain.model.KlineResponse
import cryptoautotrading.domain.model.TickerResponse

/**
 * 市場データを取得するインターフェース
 */
interface MarketDataClient {
    /**
     * 最新のティッカー情報を取得する
     */
    suspend fun getTicker(symbol: String): TickerResponse

    /**
     * K線（ローソク足）データを取得する
     */
    suspend fun getKlines(symbol: String, interval: String, date: String): KlineResponse
}
