#!/bin/bash
cat projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/presentation/ManualRealBuyCheckMain.kt | sed '/logger.info { "手動確認終了: 注文処理が実行されました (orderId 変化: $previousOrderId -> $currentOrderId)" }/!b;n;c\
\
' > temp5.kt
mv temp5.kt projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/presentation/ManualRealBuyCheckMain.kt
