#!/bin/bash
set -euo pipefail

# Cloud Scheduler が Cloud Run Job を起動するためのサービスアカウントを用意する。
#
# 作成と権限付与をひとまとめにして扱う。作成した直後のサービスアカウントは
# IAM ポリシー API からすぐには見えず、権限付与が
# 「Service account ... does not exist」で失敗することがある（結果整合性）。
# 作成には成功しているので、伝播を待って付与を繰り返せば成功する。
#
# 待っても直らない失敗（権限不足など）で繰り返しても、原因が見えなくなるだけなので、
# 伝播待ちと判断できる場合だけ再試行する。
#
# 使い方:
#   GCP_PROJECT_ID=xxx SCHEDULER_SERVICE_ACCOUNT_NAME=xxx \
#   scripts/ops/setup-scheduler-service-account.sh
#
# 終了コード: 0 = 付与済みを確認できた / 1 = 失敗

GCP_PROJECT_ID="${GCP_PROJECT_ID:-}"
SCHEDULER_SERVICE_ACCOUNT_NAME="${SCHEDULER_SERVICE_ACCOUNT_NAME:-}"
# 伝播待ちの再試行。合計で最大 GRANT_MAX_ATTEMPTS × GRANT_RETRY_SECONDS 秒待つ。
GRANT_MAX_ATTEMPTS="${GRANT_MAX_ATTEMPTS:-10}"
GRANT_RETRY_SECONDS="${GRANT_RETRY_SECONDS:-10}"

missing=""
[ -n "${GCP_PROJECT_ID}" ] || missing="${missing} GCP_PROJECT_ID"
[ -n "${SCHEDULER_SERVICE_ACCOUNT_NAME}" ] || missing="${missing} SCHEDULER_SERVICE_ACCOUNT_NAME"

if [ -n "${missing}" ]; then
  echo "エラー: 次の環境変数が未設定です:${missing}" >&2
  exit 1
fi

SA_EMAIL="${SCHEDULER_SERVICE_ACCOUNT_NAME}@${GCP_PROJECT_ID}.iam.gserviceaccount.com"

# ---------------------------------------------------------------------------
# サービスアカウントの作成
# ---------------------------------------------------------------------------

if gcloud iam service-accounts describe "${SA_EMAIL}" \
  --project "${GCP_PROJECT_ID}" >/dev/null 2>&1; then
  echo "サービスアカウントは既に存在します: ${SA_EMAIL}"
else
  echo "サービスアカウントを作成します: ${SA_EMAIL}"
  gcloud iam service-accounts create "${SCHEDULER_SERVICE_ACCOUNT_NAME}" \
    --project "${GCP_PROJECT_ID}" \
    --description="Service Account for Cloud Scheduler Job"
fi

# ---------------------------------------------------------------------------
# roles/run.invoker の付与
#
# 将来的には最小権限へ寄せるため、プロジェクト単位ではなく
# Cloud Run Job 単位などに絞るのが望ましい。
# ---------------------------------------------------------------------------

echo "roles/run.invoker を付与します。"

attempt=1
while true; do
  # 付与に成功するとポリシー全体が出力されるため、標準出力は捨てる。
  # 失敗した理由は判断に使うので、標準エラー出力だけを受け取る。
  if grant_error=$(gcloud projects add-iam-policy-binding "${GCP_PROJECT_ID}" \
    --member="serviceAccount:${SA_EMAIL}" \
    --role="roles/run.invoker" 2>&1 >/dev/null); then
    echo "付与しました。"
    break
  fi

  # 作成直後の伝播待ちだけを再試行の対象にする。
  if ! echo "${grant_error}" | grep -q "does not exist"; then
    echo "エラー: roles/run.invoker を付与できませんでした。" >&2
    echo "${grant_error}" >&2
    exit 1
  fi

  if [ "${attempt}" -ge "${GRANT_MAX_ATTEMPTS}" ]; then
    echo "エラー: ${attempt} 回試しましたが、サービスアカウントが IAM から見えません。" >&2
    echo "${grant_error}" >&2
    exit 1
  fi

  echo "サービスアカウントがまだ IAM に反映されていません。${GRANT_RETRY_SECONDS} 秒後に再試行します（${attempt}/${GRANT_MAX_ATTEMPTS}）。"
  sleep "${GRANT_RETRY_SECONDS}"
  attempt=$((attempt + 1))
done

# ---------------------------------------------------------------------------
# 付与できたことの確認
#
# 付与したつもりで先に進むと、スケジューラが 403 で失敗し続ける状態に気付けない。
# ---------------------------------------------------------------------------

granted_roles=$(gcloud projects get-iam-policy "${GCP_PROJECT_ID}" \
  --flatten="bindings[].members" \
  --filter="bindings.members:serviceAccount:${SA_EMAIL} AND bindings.role:roles/run.invoker" \
  --format="value(bindings.role)")

if [ -z "${granted_roles}" ]; then
  echo "エラー: 付与は成功しましたが、roles/run.invoker を確認できませんでした。" >&2
  exit 1
fi

echo "確認しました: ${SA_EMAIL} に roles/run.invoker が付与されています。"
