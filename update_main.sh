#!/bin/bash
cat projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/presentation/ManualRealBuyCheckMain.kt | sed 's/fun main() = runBlocking {/fun main() = runBlocking {\n    val manualConfirm = System.getenv("MANUAL_REAL_BUY_CONFIRM")\n    if (manualConfirm != "yes") {\n        logger.error { "環境変数 MANUAL_REAL_BUY_CONFIRM=yes が設定されていません。誤操作防止のため手動確認を中止します。" }\n        exitProcess(1)\n    }\n/' > temp.kt
mv temp.kt projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/presentation/ManualRealBuyCheckMain.kt
