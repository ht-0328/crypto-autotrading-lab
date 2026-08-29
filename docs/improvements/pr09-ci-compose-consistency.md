# PR09: CI / Compose の整合と安全側固定

**状態**: 未着手

## 対象の指摘

[findings.md](findings.md) の **G** / **U** / **AA**

## なぜ直すか

- **U（重要度: 高）**: README が案内する `docker compose -f docker/compose/local.yml up --build` が危険です。アプリは1回実行して終了するバッチなのに `restart: unless-stopped` が付いているため無限に再起動し、設定は本物の GMO Public API を向いています（`API_BASE_URL` は実装が読まないため無視され、[application-gmo.yaml](../../config/application-gmo.yaml) の `https://api.coin.z.com/public` が使われる）。同時に起動する WireMock は Public API には使われません。
- **G（重要度: 中）**: ワークフローの `strategy_name` 選択肢が実装済み5戦略とズレています。`ci.yml` は2件、`deploy-gcp.yml` と `backtest-smoke.yml` は `AtrTrendConfirmReboundStrategy` が欠落。
- **AA（重要度: 低）**: `ci.yml` が `./gradlew build`（テスト込み）の直後に `./gradlew test --tests "*Architecture*"` を再実行していて二重です。

## 変更対象

| ファイル | 変更内容 |
| --- | --- |
| [docker/compose/local.yml](../../docker/compose/local.yml) | `restart` / `version` 削除、WireMock をピン留め、環境変数名を修正、既定を WireMock 向けに |
| [.github/workflows/ci.yml](../../.github/workflows/ci.yml) | 戦略の選択肢を5件に、重複ステップを削除 |
| [.github/workflows/deploy-gcp.yml](../../.github/workflows/deploy-gcp.yml) | 戦略の選択肢を5件に |
| [.github/workflows/backtest-smoke.yml](../../.github/workflows/backtest-smoke.yml) | 戦略の選択肢を5件に |
| [TradingApplicationTest.kt](../../projects/crypto-autotrading-app/src/test/kotlin/cryptoautotrading/application/TradingApplicationTest.kt) | 戦略生成のテストを追加 |
| [README.md](../../README.md) | Docker Compose 手順を更新 |

## 実施手順

### 1. docker/compose/local.yml を直す

- `version: "3.8"` を削除する（現行の Compose では不要かつ警告が出る）。
- `restart: unless-stopped` を削除する。1回実行して終了するバッチであり、無限再起動して本物の API を叩き続けるため。
- `wiremock/wiremock:latest` を `wiremock/wiremock:3.5.2` にピン留めする（[ci.yml](../../.github/workflows/ci.yml) と揃える）。
- 実装が読まない環境変数を修正する。

  ```yaml
  - API_PUBLIC_BASE_URL=${API_PUBLIC_BASE_URL:-http://wiremock:8080/public}
  - API_PRIVATE_BASE_URL=${API_PRIVATE_BASE_URL:-http://wiremock:8080/private}
  ```

  既定を WireMock 向けにするのが要点です。README の手順をそのまま実行した人が本物の GMO API を叩かないようにします。
- `APP_CONFIG_PATH` は `/app/config/application-wiremock.yaml` に変更する（既定を WireMock に寄せるため）。
- `config` のマウントを読み取り専用（`:ro`）にする。アプリが設定を書き換えることはないため。

### 2. ワークフローの戦略選択肢を揃える

3ファイルとも、選択肢を実装済みの5戦略にする。

```yaml
options:
  - SafeReboundStrategy
  - CooldownReboundStrategy
  - TrendConfirmReboundStrategy
  - AtrTrendConfirmReboundStrategy
  - SimpleContrarianStrategy
```

正は [TradingApplication.createStrategy()](../../projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/application/TradingApplication.kt) の `when` 式です。

### 3. ズレを検知するテストを追加する

選択肢と実装が再びズレないよう、[TradingApplicationTest.kt](../../projects/crypto-autotrading-app/src/test/kotlin/cryptoautotrading/application/TradingApplicationTest.kt) に追加する。

- 5つの戦略名すべてで `TradingApplication` が生成・実行できること
- 未知の戦略名を渡すと例外になること（現在は `error()`）

### 4. 重複した CI ステップを削除する

[ci.yml](../../.github/workflows/ci.yml) の「Run Architecture Rule Tests」ステップを削除する。`./gradlew build` に Konsist の `ArchitectureTest` が含まれているため。

### 5. README を更新する

Docker Compose の節に、既定で WireMock に接続すること、本物の Public API を使う場合は `API_PUBLIC_BASE_URL` を明示することを書く。

## 受け入れ条件

- [ ] `docker compose -f docker/compose/local.yml up --build` が1回だけ実行して終了すること（再起動ループしないこと）
- [ ] 既定で WireMock に接続すること（本物の GMO API を叩かないこと）
- [ ] 3つのワークフローの戦略選択肢が実装済みの5件と一致していること
- [ ] 戦略名のズレを検知するテストがあること
- [ ] `ci.yml` に重複した Architecture テストのステップが無いこと

## 検証

```bash
cd projects/crypto-autotrading-app
./gradlew build

cd /workspace
docker compose -f docker/compose/local.yml up --build
# アプリが1回実行して終了すること。再起動が繰り返されないこと
grep '採用したAPIベースURL' data/app.log   # wiremock を指していること
docker compose -f docker/compose/local.yml down
```

ワークフローの選択肢は、GitHub の Actions 画面で `workflow_dispatch` の入力候補に5件出ることを確認する。

## スコープ外

- `ci.yml` の `dry_run` 入力の削除（[pr05-phase1-real-order-guard.md](pr05-phase1-real-order-guard.md)）
- Dockerfile への設定ファイル同梱（[pr02-cloud-run-config.md](pr02-cloud-run-config.md)）
- `docker-clean.sh` の修正（[pr01-docker-clean-scope.md](pr01-docker-clean-scope.md)）
