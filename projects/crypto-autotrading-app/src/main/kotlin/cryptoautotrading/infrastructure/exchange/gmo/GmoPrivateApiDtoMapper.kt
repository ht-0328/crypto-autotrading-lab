package cryptoautotrading.infrastructure.exchange.gmo

import cryptoautotrading.domain.model.order.AcceptedOrder
import cryptoautotrading.domain.model.order.ExchangeActiveOrder
import cryptoautotrading.domain.model.order.ExchangeAsset
import cryptoautotrading.domain.model.order.ExchangeOrderStatus
import cryptoautotrading.domain.model.order.ExecutedOrder
import cryptoautotrading.infrastructure.exchange.gmo.dto.*
import java.math.BigDecimal

/**
 * GMO Private API のDTOをアプリ内モデルへ変換するMapper。
 * HTTP通信は行わない。
 * 実通信を行う RealTradingExchange は、次の実装PR以降で追加する。
 */
class GmoPrivateApiDtoMapper {

    /**
     * GmoAccountAssetDto のリストを ExchangeAsset のリストに変換する。
     *
     * @param dtoList 変換元のDTOリスト
     * @return 変換後のドメインモデルリスト
     */
    fun mapToExchangeAssets(dtoList: List<GmoAccountAssetDto>): List<ExchangeAsset> {
        return dtoList.map { dto ->
            ExchangeAsset(
                symbol = dto.symbol,
                amount = BigDecimal(dto.amount),
                available = BigDecimal(dto.available),
                conversionRate = BigDecimal(dto.conversionRate)
            )
        }
    }

    /**
     * GmoActiveOrderDto のリストを ExchangeActiveOrder のリストに変換する。
     *
     * @param dtoList 変換元のDTOリスト
     * @return 変換後のドメインモデルリスト
     */
    fun mapToExchangeActiveOrders(dtoList: List<GmoActiveOrderDto>): List<ExchangeActiveOrder> {
        return dtoList.map { dto ->
            ExchangeActiveOrder(
                orderId = dto.orderId.toString(),
                symbol = dto.symbol,
                side = dto.side,
                size = BigDecimal(dto.size),
                executedSize = BigDecimal(dto.executedSize),
                status = dto.status
            )
        }
    }

    /**
     * GmoPlaceOrderResponseDto を AcceptedOrder に変換する。
     *
     * @param dto 変換元のDTO
     * @return 変換後のドメインモデル
     */
    fun mapToAcceptedOrder(dto: GmoPlaceOrderResponseDto): AcceptedOrder {
        return AcceptedOrder(
            orderId = dto.data ?: throw IllegalArgumentException("data が存在しません")
        )
    }

    /**
     * GmoOrderDto のリストを ExchangeOrderStatus のリストに変換する。
     *
     * @param dtoList 変換元のDTOリスト
     * @return 変換後のドメインモデルリスト
     */
    fun mapToExchangeOrderStatuses(dtoList: List<GmoOrderDto>): List<ExchangeOrderStatus> {
        return dtoList.map { dto ->
            ExchangeOrderStatus(
                orderId = dto.orderId.toString(),
                status = dto.status,
                executedSize = BigDecimal(dto.executedSize)
            )
        }
    }

    /**
     * GmoExecutionDto のリストを ExecutedOrder のリストに変換する。
     *
     * @param dtoList 変換元のDTOリスト
     * @return 変換後のドメインモデルリスト
     */
    fun mapToExecutedOrders(dtoList: List<GmoExecutionDto>): List<ExecutedOrder> {
        return dtoList.map { dto ->
            ExecutedOrder(
                executionId = dto.executionId.toString(),
                orderId = dto.orderId.toString(),
                symbol = dto.symbol,
                side = dto.side,
                actualPrice = BigDecimal(dto.price),
                actualSize = BigDecimal(dto.size),
                fee = BigDecimal(dto.fee),
                timestamp = dto.timestamp
            )
        }
    }
}
