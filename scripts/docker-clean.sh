#!/bin/bash

# このリポジトリの Docker リソースを削除するスクリプト。
#
# 既定では docker/compose/local.yml の Compose プロジェクトだけを対象にする。
# ホスト上の他プロジェクトのコンテナ・イメージ・ボリュームは削除しない。
# DevContainer 自身（.devcontainer/docker/docker-compose.yml）も対象外。

set -euo pipefail

REPO_ROOT=$(cd "$(dirname "$0")/.." && pwd)
COMPOSE_FILE="$REPO_ROOT/docker/compose/local.yml"

DRY_RUN=false
DELETE_ALL=false
ASSUME_YES=false

usage() {
  cat <<'USAGE'
使い方: scripts/docker-clean.sh [オプション]

このリポジトリの Docker リソース（docker/compose/local.yml のコンテナ・
ネットワーク・ボリュームと、Compose がビルドしたイメージ）を削除します。

オプション:
  --dry-run   削除対象を表示するだけで、実際には削除しない
  -y, --yes   確認プロンプトを省略する
  --all       ホスト上のすべての Docker リソースを削除する（危険）
  -h, --help  このヘルプを表示する

注意:
  --all はこのリポジトリ以外のコンテナ・イメージ・ボリュームも削除します。
  DevContainer 内で実行した場合は、開発環境自体も削除対象になります。
USAGE
}

while [ $# -gt 0 ]; do
  case "$1" in
    --dry-run) DRY_RUN=true ;;
    -y|--yes) ASSUME_YES=true ;;
    --all) DELETE_ALL=true ;;
    -h|--help) usage; exit 0 ;;
    *) echo "不明なオプション: $1" >&2; usage >&2; exit 1 ;;
  esac
  shift
done

# 対象を表示したうえで実行してよいか確認する。
# $1: 確認メッセージ、$2: 入力させる文字列（空なら y/N で確認する）
confirm() {
  local message="$1"
  local required_word="${2:-}"

  if [ "$ASSUME_YES" = true ]; then
    return 0
  fi

  if [ -n "$required_word" ]; then
    echo "$message"
    read -r -p "続行するには ${required_word} と入力してください > " answer
    [ "$answer" = "$required_word" ]
  else
    read -r -p "$message [y/N] > " answer
    case "$answer" in
      y|Y|yes|YES) return 0 ;;
      *) return 1 ;;
    esac
  fi
}

require_docker() {
  if ! command -v docker > /dev/null 2>&1; then
    echo "docker コマンドが見つかりません。Docker を利用できる環境で実行してください。" >&2
    exit 1
  fi
}

clean_repository_resources() {
  if [ ! -f "$COMPOSE_FILE" ]; then
    echo "Compose ファイルが見つかりません: $COMPOSE_FILE" >&2
    exit 1
  fi

  echo "対象: $COMPOSE_FILE の Compose プロジェクト"
  echo
  echo "🔸 コンテナ"
  docker compose -f "$COMPOSE_FILE" ps -a || true
  echo
  echo "🔸 Compose がビルドしたイメージ（--rmi local の対象）"
  docker compose -f "$COMPOSE_FILE" images || true
  echo

  if [ "$DRY_RUN" = true ]; then
    echo "（--dry-run のため削除しません）"
    return 0
  fi

  if ! confirm "上記のコンテナ・ネットワーク・ボリューム・イメージを削除します。"; then
    echo "中止しました。"
    return 0
  fi

  docker compose -f "$COMPOSE_FILE" down --volumes --rmi local --remove-orphans
  echo "✅ このリポジトリの Docker リソースを削除しました。"
}

clean_all_resources() {
  echo "⚠️  --all が指定されました。ホスト上のすべての Docker リソースを削除します。"
  echo "    このリポジトリ以外のコンテナ・イメージ・ボリュームも消えます。"
  echo
  echo "🔸 コンテナ"
  docker ps -a --format 'table {{.Names}}\t{{.Image}}\t{{.Status}}' || true
  echo
  echo "🔸 イメージ"
  docker images --format 'table {{.Repository}}\t{{.Tag}}\t{{.Size}}' || true
  echo
  echo "🔸 ボリューム"
  docker volume ls --format 'table {{.Name}}\t{{.Driver}}' || true
  echo

  if [ "$DRY_RUN" = true ]; then
    echo "（--dry-run のため削除しません）"
    return 0
  fi

  if ! confirm "本当にすべて削除しますか？" "DELETE-ALL"; then
    echo "中止しました。"
    return 0
  fi

  echo "🔸 コンテナ削除中..."
  docker rm -f $(docker ps -aq) 2>/dev/null || echo "（削除対象なし）"

  echo "🔸 イメージ削除中..."
  docker rmi -f $(docker images -aq) 2>/dev/null || echo "（削除対象なし）"

  echo "🔸 ボリューム削除中..."
  docker volume rm -f $(docker volume ls -q) 2>/dev/null || echo "（削除対象なし）"

  echo "🔸 ネットワーク削除中..."
  docker network rm $(docker network ls --format '{{.Name}}' | grep -v -E '^bridge$|^host$|^none$') 2>/dev/null || echo "（削除対象なし）"

  echo "🔸 ビルドキャッシュ削除中..."
  docker builder prune -af

  echo "🔸 システム全体クリーンアップ中..."
  docker system prune -a --volumes -f

  echo "✅ すべての Docker リソースを削除しました。"
}

require_docker

if [ "$DELETE_ALL" = true ]; then
  clean_all_resources
else
  clean_repository_resources
fi
