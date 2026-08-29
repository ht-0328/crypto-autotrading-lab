# PR03: Private API レスポンスのログ流出を止める

**状態**: 実施済み（ブランチ `fix/private-api-log-leak`）

## 対象の指摘

[findings.md](findings.md) の **M**（重要度: 高）

## なぜ直すか

[GmoPrivateApiClientImpl.kt](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/infrastructure/exchange/gmo/GmoPrivateApiClientImpl.kt) が、GMO Private API のレスポンス本文を INFO レベルでそのまま出力しています。

```kotlin
logger.info { "GMO Private API raw response: $responseText" }
```

口座残高・保有数量・注文内容が `APP_DATA_DIR/app.log` に平文で残ります。[AGENTS.md](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/AGENTS.md) の絶対厳守事項「秘密情報を…ログにも出しません」に抵触します。

**[pr02-cloud-run-config.md](pr02-cloud-run-config.md) より先に実施してください。** 先に標準出力へのログ出力を追加すると、この情報が Cloud Logging にも載ります。

## 変更対象

| ファイル | 変更内容 |
| --- | --- |
| [GmoPrivateApiClientImpl.kt](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/infrastructure/exchange/gmo/GmoPrivateApiClientImpl.kt) | レスポンス本文の出力を削除。既定では出さない |
| [.agents/skills/code-review/SKILL.md](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/.agents/skills/code-review/SKILL.md) | 「ログと秘密情報」の観点に Private API レスポンスも対象である旨を追記 |

## 実施手順

1. `GmoPrivateApiClientImpl.kt` の該当箇所を洗い出す。

   ```bash
   grep -n 'responseText' projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/infrastructure/exchange/gmo/GmoPrivateApiClientImpl.kt
   ```

   現時点では 54, 58, 64, 76, 80, 89, 121, 129, 144, 150, 165, 171 行付近に、レスポンス本文をログ・例外メッセージへ入れている箇所があります。

2. 各箇所を、本文を含まない形に置き換える。残してよいのは次の情報だけ。
   - HTTP ステータスコード
   - GMO API が返すエラーコード（`messages[].message_code`）
   - 呼び出したエンドポイントの種別（パス。クエリの値は含めない）

3. 調査用に本文が必要な場合は、`logger.debug` に限定する。既定のログレベルは `info`（[logback.xml](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/resources/logback.xml) の `${LOG_LEVEL:-info}`）なので、既定では出力されない。

4. 例外メッセージにもレスポンス本文を含めない。スタックトレースがログに出るため。

5. `code-review` スキルのチェック観点に追記する。

## 受け入れ条件

- [ ] `logger.info` / `logger.warn` / `logger.error` および例外メッセージに、Private API のレスポンス本文が含まれないこと
- [ ] `LOG_LEVEL=debug` のときだけ本文が出ること
- [ ] エラー時に、原因調査に必要な HTTP ステータスと GMO のエラーコードは残っていること
- [ ] 既存テスト（[GmoPrivateApiClientImplTest.kt](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/test/kotlin/cryptoautotrading/infrastructure/exchange/gmo/GmoPrivateApiClientImplTest.kt)）が通ること

## 検証

```bash
cd projects/crypto-autotrading-app
./gradlew build
```

加えて、WireMock 環境で Private API を呼び、ログに残高が出ないことを確認する。

```bash
# WireMock を起動した状態で
./scripts/local/run-devcontainer-menu.sh   # 2) リアルPrivate APIで残高確認 は使わない

# 生成された app.log に残高・注文情報が含まれないこと
grep -iE 'available|amount|orderId|"data"' data/local-devcontainer/app.log
```

## スコープ外

- 標準出力へのログ出力の追加（[pr02-cloud-run-config.md](pr02-cloud-run-config.md)）
- HTTP ステータス検証やリトライ方針の見直し（[backlog.md](backlog.md) の指摘 AC）
