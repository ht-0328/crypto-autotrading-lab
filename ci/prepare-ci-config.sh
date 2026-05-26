#!/bin/bash
set -euo pipefail

# CI用設定ディレクトリの作成
mkdir -p build/ci-config

# デフォルト値の決定
STRATEGY_NAME="${INPUT_STRATEGY_NAME:-SafeReboundStrategy}"
PUBLIC_API_SOURCE="${INPUT_PUBLIC_API_SOURCE:-wiremock}"
DRY_RUN="${INPUT_DRY_RUN:-true}"

echo "使用する売買戦略: ${STRATEGY_NAME}"
echo "Public API の接続先: ${PUBLIC_API_SOURCE}"
echo "dry-run: ${DRY_RUN}"
echo "Private API は常に WireMock を使用します"

# application-ci.yaml を元に CI 実行用設定を作る
CONFIG_PATH="build/ci-config/application-ci-docker.yaml"
cp config/application-ci.yaml "${CONFIG_PATH}"

# 戦略の更新
sed -i "s/strategy_name:.*/strategy_name: \"${STRATEGY_NAME}\"/g" "${CONFIG_PATH}"

# Public API の接続先を切り替える
if [ "${PUBLIC_API_SOURCE}" = "real" ]; then
  sed -i "s|public_base_url:.*|public_base_url: \"https://api.coin.z.com/public\"|g" "${CONFIG_PATH}"
else
  sed -i "s|public_base_url:.*|public_base_url: \"http://wiremock:8080/public\"|g" "${CONFIG_PATH}"
fi

# Private API は常に WireMock にする
sed -i "s|private_base_url:.*|private_base_url: \"http://wiremock:8080/private\"|g" "${CONFIG_PATH}"

# real_trading 設定を dry_run に合わせて追加する
if [ "${DRY_RUN}" = "true" ]; then
  REAL_TRADE_ENABLED="false"
else
  REAL_TRADE_ENABLED="true"
fi

{
  printf "\n"
  printf "real_trading:\n"
  printf "  dry_run: %s\n" "${DRY_RUN}"
  printf "  real_trade_enabled: %s\n" "${REAL_TRADE_ENABLED}"
  printf "  stop_on_unconfirmed_order: true\n"
  printf "  max_order_jpy: 1000\n"
  printf "  max_daily_order_jpy: 1000\n"
  printf "  max_position_jpy: 1000\n"
} >> "${CONFIG_PATH}"

# APP_CONFIG_PATH を GitHub Actions に渡す
if [ -n "${GITHUB_ENV:-}" ]; then
  echo "APP_CONFIG_PATH=/app/${CONFIG_PATH}" >> "${GITHUB_ENV}"
else
  echo "APP_CONFIG_PATH=/app/${CONFIG_PATH}"
fi
