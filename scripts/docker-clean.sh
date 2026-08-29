#!/bin/bash

set -e

echo "🛑 すべてのDockerリソースを削除します（Docker本体は残します）"

# コンテナ削除（実行中も強制終了して削除）
echo "🔸 コンテナ削除中..."
docker rm -f $(docker ps -aq) 2>/dev/null || echo "（削除対象なし）"

# イメージ削除
echo "🔸 イメージ削除中..."
docker rmi -f $(docker images -aq) 2>/dev/null || echo "（削除対象なし）"

# ボリューム削除
echo "🔸 ボリューム削除中..."
docker volume rm -f $(docker volume ls -q) 2>/dev/null || echo "（削除対象なし）"

# カスタムネットワーク削除（bridge, host, none を除外）
echo "🔸 ネットワーク削除中..."
docker network rm $(docker network ls --format '{{.Name}}' | grep -v -E '^bridge$|^host$|^none$') 2>/dev/null || echo "（削除対象なし）"

# ビルドキャッシュ削除
echo "🔸 ビルドキャッシュ削除中..."
docker builder prune -af

# システム全体クリーンアップ
echo "🔸 システム全体クリーンアップ中..."
docker system prune -a --volumes -f

echo "✅ 完了しました！Docker はキレイになりました。"
