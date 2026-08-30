# PR02: Cloud Run で設定が効かない・ログが残らない問題を直す

| 項目 | 内容 |
| --- | --- |
| 想定読者 | この改善を実施する開発者、AIコーディングエージェント |
| 読んだあとできること | Cloud Run で設定と環境変数が効くように直せる |
| 状態 | 実施済み（ブランチ `fix/cloud-run-config`） |
| 機密区分 | 公開可 |
| 作成者 | リポジトリ管理者 |
| 保守責任者 | リポジトリ管理者 |
| 最終確認日 | 2026-08-30 |


## 対象の指摘

[findings.md](findings.md) の **A** / **B** / **F** / **H** です。いずれも重要度は高〜中です。

## なぜ直すか

Cloud Run 上のアプリが、意図した設定でまったく動いていません。

- **A**: [deploy-gcp.yml](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/.github/workflows/deploy-gcp.yml) と [local.yml](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/docker/compose/local.yml) は `API_BASE_URL` などを渡します。しかし [ConfigLoader.kt](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/infrastructure/config/ConfigLoader.kt) が読むのは `API_PUBLIC_BASE_URL` / `API_PRIVATE_BASE_URL` です。指定した URL は無視され、隠れたデフォルト（本物の GMO API）に繋ぎます。
- **B**: [Dockerfile](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/docker/app/Dockerfile) は jar しかコピーせず、`APP_CONFIG_PATH` も未設定です。そのため Cloud Run では設定ファイルが常に見つからず `createDefaultConfig()` にフォールバックします。
- **F**: [logback.xml](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/resources/logback.xml) はファイル出力だけです。Cloud Logging にアプリログが残りません。
- **H**: [cloud-run-job.tf](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/infra/terraform/gcp/cloud-run-job.tf) に `APP_DATA_DIR` がありません。`app.log` だけがコンテナローカルに出て、Job 終了時に消えます。
  `state.json` と結果ファイルは、パス指定が絶対パスなので残ります。

## 変更対象

| ファイル | 変更内容 |
| --- | --- |
| [.github/workflows/deploy-gcp.yml](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/.github/workflows/deploy-gcp.yml) | `API_BASE_URL` を `API_PUBLIC_BASE_URL` / `API_PRIVATE_BASE_URL` に置き換える |
| [docs/operations/gcp/05-github-actions-variables.md](../operations/gcp/05-github-actions-variables.md) | GitHub Variables の変数名変更を反映 |
| [infra/terraform/gcp/cloud-run-job.tf](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/infra/terraform/gcp/cloud-run-job.tf) | `env` に `APP_DATA_DIR = "/mnt/gcs/data"` を追加 |
| [docker/app/Dockerfile](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/docker/app/Dockerfile) | ランタイムステージに `COPY config/ /app/config/` と `ENV APP_CONFIG_PATH=/app/config/application-gmo.yaml` を追加 |
| [projects/.../logback.xml](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/resources/logback.xml) | `ConsoleAppender`（STDOUT）を追加し `root` に紐づける |

## 実施手順

1. **deploy-gcp.yml の環境変数名を修正する**（97行付近）。

   ```yaml
   ENV_VARS="${ENV_VARS},API_PUBLIC_BASE_URL=${{ vars.API_PUBLIC_BASE_URL }}"
   ENV_VARS="${ENV_VARS},API_PRIVATE_BASE_URL=${{ vars.API_PRIVATE_BASE_URL }}"
   ```

   GitHub Variables 側にも新しい変数の登録が必要になる。
   [変数一覧](../operations/gcp/05-github-actions-variables.md) を更新し、旧 `API_BASE_URL` を削除する旨を書く。

2. **Terraform に `APP_DATA_DIR` を追加する**。

   ```hcl
   env {
     name  = "APP_DATA_DIR"
     value = "/mnt/gcs/data"
   }
   ```

   `volume_mounts` の `mount_path = "/mnt/gcs"` と整合していることを確認する。

3. **Dockerfile に設定ファイルを同梱する**。

   ```dockerfile
   COPY config/ /app/config/
   ENV APP_CONFIG_PATH=/app/config/application-gmo.yaml
   ```

   `ConfigLoader` は「設定ファイルを土台に環境変数で上書きする」設計である。
   土台を欠いたままの運用は、設計と食い違う。同梱する [application-gmo.yaml](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/config/application-gmo.yaml) は `dry_run: true` / `real_trade_enabled: false` なので安全側です。

4. **logback.xml に標準出力を追加する**。既存の `FileAppender` は残す。

   ```xml
   <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
       <encoder>
           <charset>UTF-8</charset>
           <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
       </encoder>
   </appender>
   ```

   冒頭のコメントも実態に合わせて書き換える。

## 受け入れ条件

- [ ] `docker run` でコンテナを起動したとき「設定ファイルが見つかりません」の警告が出ないこと
- [ ] `API_PUBLIC_BASE_URL` を渡すと、ログに出る採用済み URL がその値になること
- [ ] アプリのログが標準出力に出ること
- [ ] `terraform validate` が通り、`APP_DATA_DIR` が `/mnt/gcs/data` になっていること
- [ ] [変数一覧](../operations/gcp/05-github-actions-variables.md) が実装と一致していること

## 検証

```bash
cd projects/crypto-autotrading-app
./gradlew build

cd /workspace
docker build -t crypto-app:verify -f docker/app/Dockerfile .
mkdir -p data
docker run --rm \
  -e APP_DATA_DIR=/app/data \
  -e API_PUBLIC_BASE_URL=http://example.invalid/public \
  -v "$(pwd)/data:/app/data" \
  crypto-app:verify
```

確認すること。

1. 「設定ファイルが見つかりません」の警告が出ない
2. 標準出力にログが流れる
3. ログの「最終的に採用したAPIベースURL(Public)」が `http://example.invalid/public` になっている

Terraform:

```bash
cd infra/terraform/gcp
terraform fmt -check
terraform init -backend=false
terraform validate
```

## 注意

- **デプロイ前に GitHub Variables を更新すること。** 変数名を変えます。`API_PUBLIC_BASE_URL` と `API_PRIVATE_BASE_URL` を先に登録してください。登録前は空文字が渡り、設定ファイルの値が使われます。動作は安全側ですが、意図した URL にはなりません。

## スコープ外

- `order_sizing_mode` の環境変数上書き追加（[PR10](pr10-config-fail-fast.md)）
- gcloud と Terraform の環境変数集合の突き合わせ（[PR10](pr10-config-fail-fast.md)）
- [docker/compose/local.yml](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/docker/compose/local.yml) の修正（[PR09](pr09-ci-compose-consistency.md)）
