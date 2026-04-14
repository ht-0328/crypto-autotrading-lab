# crypto-autotrading-lab

仮想通貨の自動売買アプリを段階的に開発するためのリポジトリです。
最終目的は自動化による利益獲得ですが、初期段階（Phase1）は安全を優先し、実注文なしのシミュレーション環境を構築します。

## Phase1 の目標

Phase1では以下の機能を持つKotlin CLIアプリを作成します：
* 取引所: GMOコイン (BTC)
* 現在価格と過去足データの取得
* 売買判定結果のコンソール出力
* 実行結果のCSV保存と状態のJSON保存
* ローカルDocker環境での定期実行 (5分ごと)

## ドキュメント

各仕様や開発手順については以下のドキュメントを参照してください。

### 仕様書
* [Phase1 仕様書](docs/specs/phase1.md)
* [ロードマップ (Phase2以降)](docs/specs/roadmap.md)

### 開発環境
* [開発環境セットアップ手順](docs/setup/development.md)

## 起動方法 (ローカル実行)

本番デプロイ用ではなく、ローカルでの実行確認用として以下のコマンドで起動できます。

```bash
docker compose -f docker/compose/local.yml up --build
```
