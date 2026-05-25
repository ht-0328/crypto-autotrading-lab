#!/bin/bash
set -euo pipefail

DRY_RUN="${INPUT_DRY_RUN:-true}"
PUBLIC_API_SOURCE="${INPUT_PUBLIC_API_SOURCE:-wiremock}"

if [ "${DRY_RUN}" = "false" ] && [ "${PUBLIC_API_SOURCE}" = "wiremock" ]; then
  echo "WireMockのPrivate API呼び出しを確認します..."
  JOURNAL=$(docker run --rm --network crypto-ci-net curlimages/curl:latest -s http://wiremock:8080/__admin/requests)

  if ! echo "${JOURNAL}" | grep -q "/private/v1/account/assets"; then
    echo "エラー: GET /private/v1/account/assets が呼び出されていません"
    exit 1
  fi

  if ! echo "${JOURNAL}" | grep -q "/private/v1/activeOrders"; then
    echo "エラー: GET /private/v1/activeOrders が呼び出されていません"
    exit 1
  fi

  if ! echo "${JOURNAL}" | grep -q "/private/v1/order"; then
    echo "エラー: POST /private/v1/order が呼び出されていません"
    exit 1
  fi

  echo "期待されたすべてのPrivate APIがWireMockへ呼び出されたことを確認しました"
else
  echo "条件を満たさないため、WireMockのPrivate API呼び出し確認をスキップします (dry_run=${DRY_RUN}, public_api_source=${PUBLIC_API_SOURCE})"
fi
