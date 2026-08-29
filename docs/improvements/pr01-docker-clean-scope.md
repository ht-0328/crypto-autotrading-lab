# PR01: ホストの Docker を全消しするスクリプトを安全にする

| 項目 | 内容 |
| --- | --- |
| 想定読者 | この改善を実施する開発者、AIコーディングエージェント |
| 読んだあとできること | 削除範囲の限定と確認プロンプトの追加を、単体で実施できる |
| 状態 | 実施済み（ブランチ `fix/docker-clean-scope`） |
| 機密区分 | 公開可 |
| 作成者 | リポジトリ管理者 |
| 保守責任者 | リポジトリ管理者 |
| 最終確認日 | 2026-08-30 |


## 対象の指摘

[findings.md](findings.md) の **N**（重要度: 高）

## なぜ直すか

[scripts/docker-clean.sh](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/scripts/docker-clean.sh) が、ホスト上の全コンテナ・全イメージ・全ボリューム・全カスタムネットワーク・全ビルドキャッシュを確認なしで削除していました（`docker system prune -a --volumes -f` まで実行）。他プロジェクトのデータも消え、DevContainer 内から実行すれば開発環境自体も削除対象になります。README にも説明がなく、実行1回で被害が出ます。

## 変更対象

| ファイル | 変更内容 |
| --- | --- |
| [scripts/docker-clean.sh](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/scripts/docker-clean.sh) | 削除範囲をこのリポジトリの Compose プロジェクトに限定。`--dry-run` / `-y` / `--all` / `--help` を追加 |
| [README.md](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/README.md) | `scripts/` をリポジトリ構成に追記し、クリーンアップ手順と `--all` の危険性を明記 |

## 実施手順

1. `scripts/docker-clean.sh` の既定動作を `docker compose -f docker/compose/local.yml down --volumes --rmi local --remove-orphans` に置き換える。
   - `-p` は指定しない。README の起動手順 `docker compose -f docker/compose/local.yml up --build` と同じ既定プロジェクト名を使うため。
2. 削除前に対象のコンテナとイメージを一覧表示し、`y/N` の確認を取る。
3. オプションを追加する。
   - `--dry-run`: 対象を表示するだけで削除しない
   - `-y` / `--yes`: 確認プロンプトを省略
   - `--all`: 全削除。実行前に `DELETE-ALL` の入力を必須にする
   - `-h` / `--help`: 使い方を表示
4. 冒頭で `docker` コマンドの存在を確認し、無ければエラー終了する。
5. 実行ビットを `100755` にする（他のスクリプトと揃える。従来は `100644` だった）。
6. README に「Docker リソースのクリーンアップ」節を追加する。

## 受け入れ条件

- [ ] 既定実行で削除されるのが `docker/compose/local.yml` のリソースだけであること
- [ ] `--dry-run` で何も削除されないこと
- [ ] `--all` が `DELETE-ALL` の入力なしに実行されないこと
- [ ] DevContainer 自身（`.devcontainer/docker/docker-compose.yml`）が削除対象に含まれないこと
- [ ] README にスクリプトの説明と `--all` の危険性が書かれていること

## 検証

```bash
bash -n scripts/docker-clean.sh
./scripts/docker-clean.sh --help

# 無関係なコンテナを1つ起動しておく
docker run -d --name unrelated-check alpine sleep 600

./scripts/docker-clean.sh --dry-run   # 対象に unrelated-check が含まれないこと
./scripts/docker-clean.sh             # 確認プロンプトが出ること
docker ps -a | grep unrelated-check   # 実行後も残っていること

docker rm -f unrelated-check
```

`./gradlew build` は不要（Kotlin / Gradle / `config/` に変更がないため）。

## 実施結果

- 確認済み: 構文チェック・`--help`・不明オプション（exit 1）・`--dry-run` の分岐・`./scripts/docker-clean.sh` の直接実行
- **未検証**: 実際の削除挙動。作業時の環境に `docker` コマンドが無かったため。DevContainer 上で上記の検証手順を一度実行してください。

## スコープ外

- [docker/compose/local.yml](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/docker/compose/local.yml) 自体の修正（`restart: unless-stopped` の削除など）は [PR09](pr09-ci-compose-consistency.md) で行う。
