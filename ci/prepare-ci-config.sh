#!/bin/bash
set -euo pipefail

# CI 実行用の環境変数を決めて GitHub Actions に渡す。
#
# 設定ファイル（config/application-ci.yaml）は書き換えない。
# ConfigLoader は「設定ファイルを土台に環境変数で上書きする」設計なので、
# 切り替えたい項目は環境変数で渡せば足りる。

# デフォルト値の決定
STRATEGY_NAME="${INPUT_STRATEGY_NAME:-SafeReboundStrategy}"
PUBLIC_API_SOURCE="${INPUT_PUBLIC_API_SOURCE:-wiremock}"

# Public API の接続先を切り替える
if [ "${PUBLIC_API_SOURCE}" = "real" ]; then
  PUBLIC_BASE_URL="https://api.coin.z.com/public"
else
  PUBLIC_BASE_URL="http://wiremock:8080/public"
fi

# Private API は常に WireMock にする
PRIVATE_BASE_URL="http://wiremock:8080/private"

echo "使用する売買戦略: ${STRATEGY_NAME}"
echo "Public API の接続先: ${PUBLIC_BASE_URL}"
echo "Private API の接続先: ${PRIVATE_BASE_URL}（常に WireMock）"
echo "dry-run: true（Phase1 では固定。切り替えできません）"

# Phase1 では実注文を禁止しているため、実取引の設定は常に無効で固定する
write_env() {
  local line="$1"
  echo "$line"
  if [ -n "${GITHUB_ENV:-}" ]; then
    echo "$line" >> "${GITHUB_ENV}"
  fi
}

write_env "APP_CONFIG_PATH=/app/config/application-ci.yaml"
write_env "APP_TRADING_STRATEGY_NAME=${STRATEGY_NAME}"
write_env "API_PUBLIC_BASE_URL=${PUBLIC_BASE_URL}"
write_env "API_PRIVATE_BASE_URL=${PRIVATE_BASE_URL}"
write_env "REAL_TRADING_DRY_RUN=true"
write_env "REAL_TRADING_ENABLED=false"
write_env "REAL_TRADING_MAX_ORDER_JPY=1000"
write_env "REAL_TRADING_MAX_DAILY_ORDER_JPY=1000"
write_env "REAL_TRADING_MAX_POSITION_JPY=1000"
