# PR10: 設定の fail-fast と環境変数契約の統一

**状態**: 実施済み（ブランチ `fix/config-fail-fast`）

## 対象の指摘

[findings.md](findings.md) の **T** / **B**（残り） / **D** / **Z** / **C**（乖離解消のみ） / **AD** / **AE**（作業中に発見）

## なぜ直すか

- **T（中）**: [ConfigLoader.kt](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/infrastructure/config/ConfigLoader.kt) が `toIntOrNull()` / `toBooleanStrictOrNull()` を使っており、環境変数の書き間違いが黙ってベース値へフォールバックします。運用者は設定に失敗したことに気付けません。
- **B の残り（高）**: `order_sizing_mode` だけ環境変数で上書きできず、Cloud Run では設定ファイルの値から変更できません。
- **D（低）**: `stop_on_unconfirmed_order` は読み込まれるだけで、どの判定にも使われていません（[RealTradingSafetyChecker](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/realtrading/RealTradingSafetyChecker.kt) は値に関係なく常に停止する）。設定項目の意味と実装が一致していません。
- **Z（低）**: 設定の切り替えが sed による YAML 書き換えで2箇所に散在しています。`ConfigLoader` は環境変数上書きに対応済みなので sed は不要です。
- **C（中、乖離解消のみ）**: gcloud と Terraform で Cloud Run Job に渡す環境変数の集合が食い違っています。一本化は [backlog.md](backlog.md) 送りですが、食い違いだけは消します。
- **AD（中、作業中に発見）**: Terraform の `output_path` / `state_path` の既定値が絶対パスですが、アプリは `Paths.get(APP_DATA_DIR, statePath)` で連結するため `/mnt/gcs/data/mnt/gcs/data/state.json` になります。`terraform apply` を運用していないため実害は出ていません。
- **AE（高、作業中に発見）**: 文字列の環境変数が空文字でも値として採用されます。`deploy-gcp.yml` は未登録の GitHub Variable を空文字で渡すため、[pr02](pr02-cloud-run-config.md) で `API_PUBLIC_BASE_URL` を実際に読むようにしたことで「API のベースURLが空のまま起動する」経路ができていました。

## 変更対象

| ファイル | 変更内容 |
| --- | --- |
| [ConfigLoader.kt](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/infrastructure/config/ConfigLoader.kt) | パース失敗を起動時例外に。`TRADING_ORDER_SIZING_MODE` を追加 |
| [ConfigLoaderTest.kt](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/test/kotlin/cryptoautotrading/infrastructure/config/ConfigLoaderTest.kt) | 追加ケース |
| [RealTradingConfig.kt](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/model/realtrading/RealTradingConfig.kt) | `stopOnUnconfirmedOrder` の KDoc を実態に合わせる |
| [ci/prepare-ci-config.sh](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/ci/prepare-ci-config.sh) | sed を環境変数の export に置き換える |
| [scripts/local/run-devcontainer-menu.sh](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/scripts/local/run-devcontainer-menu.sh) | 同上 |
| [.github/workflows/deploy-gcp.yml](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/.github/workflows/deploy-gcp.yml) / [cloud-run-job.tf](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/infra/terraform/gcp/cloud-run-job.tf) | 環境変数の集合を一致させる |
| [infra/terraform/gcp/README.md](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/infra/terraform/gcp/README.md) | 現状どちらが正かを明記 |

## 実施手順

### 1. 環境変数のパースを fail-fast にする

`ConfigLoader.overrideWithEnvVars()` で、**環境変数が空でないのに変換できない場合は起動時例外**にする。

```kotlin
private fun requireInt(name: String, base: Int): Int {
    val raw = System.getenv(name) ?: return base
    if (raw.isBlank()) return base
    return raw.toIntOrNull() ?: error("環境変数 $name の値を数値として解釈できません")
}
```

**空文字は「未指定」として従来どおりベース値を使うこと。** [deploy-gcp.yml](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/.github/workflows/deploy-gcp.yml) は未設定の GitHub Variable を `${{ vars.X }}` で空文字として渡すため、ここを例外にすると既存デプロイが壊れます。

例外メッセージには**変数名だけ**を含め、値は含めないこと（設定値が秘密情報である可能性があるため）。

`Boolean` 用にも同様のヘルパを用意する。

### 2. order_sizing_mode を環境変数で上書きできるようにする

```kotlin
orderSizingMode = System.getenv("TRADING_ORDER_SIZING_MODE")
    ?.takeIf { it.isNotBlank() }
    ?.let { raw ->
        runCatching { OrderSizingMode.valueOf(raw) }
            .getOrElse { error("環境変数 TRADING_ORDER_SIZING_MODE の値が不正です") }
    }
    ?: base.trading.orderSizingMode
```

[cloud-run-job.tf](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/infra/terraform/gcp/cloud-run-job.tf) と [deploy-gcp.yml](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/.github/workflows/deploy-gcp.yml) からも渡せるようにする。

### 3. stop_on_unconfirmed_order の扱いを決める

**設定キーは残し、実装は現行の「常に停止」を維持します。** [AGENTS.md](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/AGENTS.md) の「既存の公開API、設定キー、その意味を壊しません」に従うためと、`false` を実際に効かせると未確認注文がある状態でも発注できてしまい安全側に倒す原則に反するためです。

- [RealTradingConfig.kt](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/domain/model/realtrading/RealTradingConfig.kt) の KDoc に「Phase1〜Phase3 では値に関わらず常に停止する。`false` は将来用の予約」と書く。
- `false` が指定された場合は起動時に警告ログを出す。
- [real-trading-gmo-order.md](../specifications/features/real-trading-gmo-order.md) の該当箇所にも同じ内容を書く。

### 4. sed による設定書き換えをやめる

- [ci/prepare-ci-config.sh](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/ci/prepare-ci-config.sh): `strategy_name` / `public_base_url` / `private_base_url` の sed を削除し、`APP_TRADING_STRATEGY_NAME` / `API_PUBLIC_BASE_URL` / `API_PRIVATE_BASE_URL` を `GITHUB_ENV` に書き出す形にする。設定ファイルは `config/application-ci.yaml` をそのまま使う。
- [scripts/local/run-devcontainer-menu.sh](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/scripts/local/run-devcontainer-menu.sh): `application-runtime.yaml` の生成をやめ、`API_PUBLIC_BASE_URL` を export して `config/application-gmo.yaml` をそのまま使う。README の「設定ファイルの扱い」の記述も更新する。

### 5. gcloud と Terraform の環境変数を一致させる

両者が Cloud Run Job に渡す環境変数の集合を突き合わせ、片方にしかないものを埋める。

- `deploy-gcp.yml` に不足: `TRADING_COOLDOWN_LENGTH`, `TRADING_ATR_*`, `REAL_TRADING_*`, `TRADING_ORDER_SIZING_MODE`
- 双方に `APP_DATA_DIR` があること（[pr02-cloud-run-config.md](pr02-cloud-run-config.md) で Terraform 側に追加済みのはず）

[infra/terraform/gcp/README.md](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/infra/terraform/gcp/README.md) に明記する。

> **現状**: `terraform apply` は運用していません。Cloud Run Job の正は GitHub Actions（`deploy-gcp.yml` の `gcloud run jobs deploy`）です。Terraform コードは同じ構成を宣言的に保つために維持しており、環境変数の集合は gcloud 側と一致させています。一本化は今後の課題です。

## 受け入れ条件

- [ ] 空でない環境変数が変換できないとき、起動時に例外になること
- [ ] 空文字の環境変数は従来どおり無視されること
- [ ] 例外メッセージに設定値が含まれないこと
- [ ] `TRADING_ORDER_SIZING_MODE` で注文サイズモードを変更できること。不正値は起動時例外
- [ ] `stop_on_unconfirmed_order: false` のとき警告が出て、動作は「常に停止」のままであること
- [ ] `prepare-ci-config.sh` と `run-devcontainer-menu.sh` が YAML を書き換えないこと
- [ ] gcloud と Terraform の環境変数の集合が一致していること

## 検証

```bash
cd projects/crypto-autotrading-app
./gradlew build
```

```bash
cd /workspace
docker build -t crypto-app:verify -f docker/app/Dockerfile .

# 不正な値で起動が失敗すること
docker run --rm -e TRADING_TRADE_AMOUNT=abc crypto-app:verify; echo "exit=$?"

# 空文字なら従来どおり動くこと
docker run --rm -e TRADING_TRADE_AMOUNT= crypto-app:verify; echo "exit=$?"

# 注文サイズモードが反映されること
docker run --rm -e TRADING_ORDER_SIZING_MODE=ALL_IN crypto-app:verify

# CI 設定生成が YAML を書き換えないこと
INPUT_STRATEGY_NAME=CooldownReboundStrategy INPUT_PUBLIC_API_SOURCE=wiremock ci/prepare-ci-config.sh
git diff --exit-code config/   # 差分が出ないこと
```

```bash
cd infra/terraform/gcp && terraform fmt -check && terraform init -backend=false && terraform validate
```

## スコープ外

- gcloud / Terraform の一本化（[backlog.md](backlog.md)）
- `RealTradingSafetyChecker` の入力値検証（[backlog.md](backlog.md) の指摘 AB）
