#!/bin/bash
cat projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/presentation/ManualRealBuyCheckMain.kt | sed '/if (!orderId.isNullOrBlank()) {/c\
                val currentOrderId = currentState.realTrading.latestOrder?.orderId\
                if (previousOrderId.isNullOrBlank() && !currentOrderId.isNullOrBlank()) {\
                    logger.info { "手動確認終了: 新規買い注文が受け付けられた可能性があります (実行前 orderId なし -> 実行後 orderId あり: $currentOrderId)" }\
                } else if (!previousOrderId.isNullOrBlank() && previousOrderId == currentOrderId) {\
                    logger.info { "手動確認終了: 既存注文の状態確認、または安全チェックにより新規注文なし (実行前後で同じ orderId: $currentOrderId)" }\
                } else if (currentOrderId.isNullOrBlank()) {\
                    logger.warn { "手動確認終了: 安全チェック等により注文なし (実行後も orderId なし)" }\
                } else {\
                    logger.info { "手動確認終了: 注文処理が実行されました (orderId 変化: $previousOrderId -> $currentOrderId)" }\
                }' > temp3.kt
mv temp3.kt projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/presentation/ManualRealBuyCheckMain.kt
