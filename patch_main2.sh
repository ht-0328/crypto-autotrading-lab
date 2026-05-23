#!/bin/bash
cat projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/presentation/ManualRealBuyCheckMain.kt | sed '/var currentState = stateRepository.load()/a\
    logger.info { "実行前の状態 - isHolding: ${currentState.isHolding}, holdingAmount: ${currentState.holdingAmount}, buyPrice: ${currentState.buyPrice}, latestOrder.orderId: ${currentState.realTrading.latestOrder?.orderId}, latestOrder.status: ${currentState.realTrading.latestOrder?.status}, symbol: ${config.trading.symbol}, tradeAmount: ${config.trading.tradeAmount}" }\
    val previousOrderId = currentState.realTrading.latestOrder?.orderId' > temp2.kt
mv temp2.kt projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/presentation/ManualRealBuyCheckMain.kt
