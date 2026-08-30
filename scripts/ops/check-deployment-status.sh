#!/bin/bash
set -euo pipefail

# デプロイ済みの本番環境が「いま実際にどう動いているか」を確認する。
#
# 確認するのは次の4点。
#   1. Cloud Run Job が実注文モードか、安全モード（dry-run / 無効）か
#   2. 実注文と通知に必要な認証情報が Secret Manager から結線されているか
#   3. Cloud Scheduler が実際にジョブを起動できているか
#   4. 直近の実行が成功しているか、自動実行が止まっていないか
#
# 「デプロイは成功したのに動いていない」状態を見つけることが目的なので、
# 確認できなかった項目は成功扱いにせず、異常として扱う（安全側に倒す）。
#
# 秘密情報は出力しない。API キーや Webhook URL は結線の有無だけを表示する。
#
# 使い方（ローカル）:
#   scripts/ops/check-deployment-status.sh
#
# プロジェクトIDやジョブ名は GitHub Variables から取得するため、通常は引数も
# 環境変数も要らない（gh へのログインが必要）。個別に上書きしたい場合は
# 環境変数で渡す:
#   CLOUD_RUN_JOB_NAME=xxx scripts/ops/check-deployment-status.sh
#
# 終了コード: 0 = 異常なし（警告は含みうる） / 1 = 異常あり

# ---------------------------------------------------------------------------
# 設定の読み取り
#
# 名前や ID の正は GitHub Variables である（デプロイもスケジューラもここを見ている）。
# ローカルでは gh 経由で取得し、確認対象がデプロイ先とずれないようにする。
# 環境変数で明示された場合はそちらを優先する。GitHub Actions からは環境変数が
# 渡るため gh は呼ばれない。
# ---------------------------------------------------------------------------

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

github_variables=""

# GitHub Variables を読み込む。取得できなければ空のままにして、環境変数だけで解決する。
#
# 解決処理はコマンド置換の中（子シェル）で呼ばれるため、そこで読み込んでも
# 結果が親シェルに残らない。呼び出しが設定の数だけ繰り返されるのを避けるため、
# ここで一度だけ読み込む。
load_github_variables() {
  command -v gh >/dev/null 2>&1 || return 0

  # gh はリポジトリの文脈を必要とするため、スクリプトの置き場所から解決する。
  local repo_root
  repo_root="$(git -C "${SCRIPT_DIR}" rev-parse --show-toplevel 2>/dev/null)" || return 0

  github_variables="$(cd "${repo_root}" && gh variable list 2>/dev/null)" || return 0
}

# 環境変数 → GitHub Variables の順で解決する。
resolve_setting() {
  local name="$1"
  local current="${!name:-}"

  if [ -n "${current}" ]; then
    echo "${current}"
    return 0
  fi

  [ -n "${github_variables}" ] || return 0
  echo "${github_variables}" | awk -F'\t' -v key="${name}" '$1 == key { print $2; exit }'
}

# 環境変数だけで足りる場合（GitHub Actions から実行した場合）は gh を呼ばない。
if [ -z "${GCP_PROJECT_ID:-}" ] ||
  [ -z "${CLOUD_RUN_JOB_NAME:-}" ] ||
  [ -z "${CLOUD_SCHEDULER_JOB_NAME:-}" ] ||
  [ -z "${SCHEDULER_SERVICE_ACCOUNT_NAME:-}" ]; then
  load_github_variables
fi

GCP_PROJECT_ID="$(resolve_setting GCP_PROJECT_ID)"
if [ -z "${GCP_PROJECT_ID}" ]; then
  GCP_PROJECT_ID="$(gcloud config get-value project 2>/dev/null || true)"
fi

GCP_REGION="$(resolve_setting GCP_REGION)"
GCP_REGION="${GCP_REGION:-asia-northeast1}"

CLOUD_RUN_JOB_NAME="$(resolve_setting CLOUD_RUN_JOB_NAME)"
CLOUD_SCHEDULER_JOB_NAME="$(resolve_setting CLOUD_SCHEDULER_JOB_NAME)"
SCHEDULER_SERVICE_ACCOUNT_NAME="$(resolve_setting SCHEDULER_SERVICE_ACCOUNT_NAME)"

# 自動実行が止まっていると判断するまでの分数。Cloud Scheduler の間隔より十分長くする。
STALE_MINUTES="${STALE_MINUTES:-30}"

missing=""
[ -n "${GCP_PROJECT_ID}" ] || missing="${missing} GCP_PROJECT_ID"
[ -n "${CLOUD_RUN_JOB_NAME}" ] || missing="${missing} CLOUD_RUN_JOB_NAME"
[ -n "${CLOUD_SCHEDULER_JOB_NAME}" ] || missing="${missing} CLOUD_SCHEDULER_JOB_NAME"
[ -n "${SCHEDULER_SERVICE_ACCOUNT_NAME}" ] || missing="${missing} SCHEDULER_SERVICE_ACCOUNT_NAME"

if [ -n "${missing}" ]; then
  echo "エラー: 次の設定を解決できませんでした:${missing}" >&2
  echo >&2
  echo "GitHub Variables から取得するには、gh にログインしてください（gh auth status で確認）。" >&2
  echo "環境変数で直接渡すこともできます:" >&2
  echo "  CLOUD_RUN_JOB_NAME=xxx CLOUD_SCHEDULER_JOB_NAME=xxx \\" >&2
  echo "  SCHEDULER_SERVICE_ACCOUNT_NAME=xxx scripts/ops/check-deployment-status.sh" >&2
  exit 1
fi

SCHEDULER_SA_EMAIL="${SCHEDULER_SERVICE_ACCOUNT_NAME}@${GCP_PROJECT_ID}.iam.gserviceaccount.com"

# ---------------------------------------------------------------------------
# 出力の補助
# ---------------------------------------------------------------------------

error_count=0
warn_count=0

section() { echo; echo "== $1 =="; }
ok()   { echo "  [OK]   $1"; }
info() { echo "  [情報] $1"; }
warn() { echo "  [警告] $1"; warn_count=$((warn_count + 1)); }
ng()   { echo "  [異常] $1"; error_count=$((error_count + 1)); }

# ---------------------------------------------------------------------------
# Cloud Run Job
# ---------------------------------------------------------------------------

if [ -n "${github_variables}" ]; then
  echo "設定の取得元: 環境変数と GitHub Variables"
else
  echo "設定の取得元: 環境変数"
fi
echo "対象: プロジェクト ${GCP_PROJECT_ID} / リージョン ${GCP_REGION}"

section "Cloud Run Job (${CLOUD_RUN_JOB_NAME} / ${GCP_REGION})"

CONTAINER_PATH="spec.template.spec.template.spec.containers[0]"

if ! job_image=$(gcloud run jobs describe "${CLOUD_RUN_JOB_NAME}" \
  --region "${GCP_REGION}" \
  --project "${GCP_PROJECT_ID}" \
  --format="value(${CONTAINER_PATH}.image)" 2>/dev/null); then
  ng "Cloud Run Job が見つからないか、参照する権限がありません。"
  echo
  echo "結果: 異常 ${error_count} 件 / 警告 ${warn_count} 件"
  exit 1
fi

info "イメージ: ${job_image}"

# 環境変数を「名前<TAB>値」の形にそろえる。
# 値が Secret Manager 参照の場合は値を出さず (secret) と表示する。
env_yaml=$(gcloud run jobs describe "${CLOUD_RUN_JOB_NAME}" \
  --region "${GCP_REGION}" \
  --project "${GCP_PROJECT_ID}" \
  --format="yaml(${CONTAINER_PATH}.env)")

env_pairs=$(echo "${env_yaml}" | awk '
  function flush() {
    if (name != "") {
      printf "%s\t%s\n", name, value
    }
    name = ""
    value = ""
  }
  /^ *- name: / {
    flush()
    name = $0
    sub(/^ *- name: /, "", name)
    next
  }
  /^ *value: / && name != "" {
    value = $0
    sub(/^ *value: /, "", value)
    gsub(/^'"'"'|'"'"'$/, "", value)
    next
  }
  /^ *valueFrom:/ && name != "" {
    value = "(secret)"
    next
  }
  END { flush() }
')

env_value() {
  echo "${env_pairs}" | awk -F'\t' -v key="$1" '$1 == key { print $2; exit }'
}

app_phase=$(env_value "APP_PHASE")
real_trading_enabled=$(env_value "REAL_TRADING_ENABLED")
real_trading_dry_run=$(env_value "REAL_TRADING_DRY_RUN")
notification_enabled=$(env_value "NOTIFICATION_ENABLED")

for key in \
  APP_PHASE \
  REAL_TRADING_ENABLED \
  REAL_TRADING_DRY_RUN \
  NOTIFICATION_ENABLED \
  APP_TRADING_STRATEGY_NAME \
  TRADING_SYMBOL \
  TRADING_TRADE_AMOUNT \
  REAL_TRADING_MAX_ORDER_JPY \
  REAL_TRADING_MAX_DAILY_ORDER_JPY \
  REAL_TRADING_MAX_POSITION_JPY \
  REAL_TRADING_MAX_DAILY_LOSS_JPY \
  REAL_TRADING_MAX_CONSECUTIVE_LOSSES \
  REAL_TRADING_MAX_SLIPPAGE_RATE
do
  value=$(env_value "${key}")
  if [ -z "${value}" ]; then
    info "${key} = （未設定。設定ファイルの値が使われます）"
  else
    info "${key} = ${value}"
  fi
done

# 実注文が有効なのは enabled=true かつ dry_run=false のときだけ。
# さらに実注文が許可されるのは Phase3 以降なので、Phase が足りなければ
# アプリは起動時に失敗する（config/application-gmo.yaml の定義に合わせる）。
if [ "${real_trading_enabled}" = "true" ] && [ "${real_trading_dry_run}" = "false" ]; then
  if [[ "${app_phase}" =~ ^[0-9]+$ ]] && [ "${app_phase}" -ge 3 ]; then
    warn "実注文モードです。実資金で発注されます（APP_PHASE=${app_phase}, enabled=true, dry_run=false）。"
  else
    ng "実注文が有効なのに APP_PHASE=${app_phase:-未設定} です。アプリは起動時に失敗します。"
  fi
else
  ok "安全モードです（enabled=${real_trading_enabled:-未設定}, dry_run=${real_trading_dry_run:-未設定}）。実注文は行われません。"
fi

# ---------------------------------------------------------------------------
# 認証情報の結線
# ---------------------------------------------------------------------------

section "認証情報の結線"

if [ "${real_trading_enabled}" = "true" ]; then
  if [ "$(env_value "GMO_API_KEY")" = "(secret)" ] && [ "$(env_value "GMO_API_SECRET")" = "(secret)" ]; then
    ok "GMO の API キーとシークレットが Secret Manager から結線されています。"
  else
    ng "実注文が有効なのに GMO の認証情報が結線されていません。"
  fi
else
  info "実注文が無効なため、GMO の認証情報は不要です。"
fi

if [ "${notification_enabled}" = "true" ]; then
  if [ "$(env_value "NOTIFICATION_WEBHOOK_URL")" = "(secret)" ]; then
    ok "通知先の Webhook URL が Secret Manager から結線されています。"
  else
    ng "通知が有効なのに Webhook URL が結線されていません。異常に気付けません。"
  fi
else
  warn "通知が無効です。異常が起きても気付けません。"
fi

# ---------------------------------------------------------------------------
# Cloud Scheduler
# ---------------------------------------------------------------------------

section "Cloud Scheduler (${CLOUD_SCHEDULER_JOB_NAME})"

# 後段の「直近の実行」でも参照するため、if の外で宣言する。
scheduler_state=""

if scheduler_info=$(gcloud scheduler jobs describe "${CLOUD_SCHEDULER_JOB_NAME}" \
  --location "${GCP_REGION}" \
  --project "${GCP_PROJECT_ID}" \
  --format='value[separator="|"](state, schedule, lastAttemptTime, status.code, httpTarget.oauthToken.serviceAccountEmail)' 2>/dev/null); then

  scheduler_state=$(echo "${scheduler_info}" | cut -d'|' -f1)
  scheduler_schedule=$(echo "${scheduler_info}" | cut -d'|' -f2)
  scheduler_last_attempt=$(echo "${scheduler_info}" | cut -d'|' -f3)
  scheduler_status_code=$(echo "${scheduler_info}" | cut -d'|' -f4)
  scheduler_sa=$(echo "${scheduler_info}" | cut -d'|' -f5)

  info "スケジュール: ${scheduler_schedule}"
  info "直近の起動要求: ${scheduler_last_attempt:-なし}"

  if [ "${scheduler_state}" = "ENABLED" ]; then
    ok "状態: ENABLED"
  else
    warn "状態: ${scheduler_state}。自動実行されません。"
  fi

  # status.code は直近の起動要求が失敗したときだけ値が入る。
  # デプロイが成功していても、ここが失敗していると一度も売買が動かない。
  if [ -n "${scheduler_status_code}" ]; then
    ng "直近の起動要求が失敗しています（status.code=${scheduler_status_code}）。ジョブは起動していません。"
  else
    ok "直近の起動要求は成功しています。"
  fi

  # 起動に使うサービスアカウントが消えていると、権限エラーではなく 404 として返る。
  if [ "${scheduler_sa}" != "${SCHEDULER_SA_EMAIL}" ]; then
    warn "起動に使うサービスアカウントが想定と異なります: ${scheduler_sa}"
  fi

  # 参照する権限が無いだけの場合を「存在しない」と誤判定しないよう、エラー内容で分ける。
  if sa_error=$(gcloud iam service-accounts describe "${SCHEDULER_SA_EMAIL}" \
    --project "${GCP_PROJECT_ID}" 2>&1 >/dev/null); then
    ok "起動用サービスアカウントが存在します: ${SCHEDULER_SA_EMAIL}"

    if invoker_roles=$(gcloud projects get-iam-policy "${GCP_PROJECT_ID}" \
      --flatten="bindings[].members" \
      --filter="bindings.members:serviceAccount:${SCHEDULER_SA_EMAIL} AND bindings.role:roles/run.invoker" \
      --format="value(bindings.role)" 2>/dev/null); then
      if [ -n "${invoker_roles}" ]; then
        ok "起動用サービスアカウントに roles/run.invoker が付与されています。"
      else
        ng "起動用サービスアカウントに roles/run.invoker がありません。起動要求は 404 で失敗します。"
      fi
    else
      warn "IAM ポリシーを参照できませんでした。run.invoker の有無は未確認です。"
    fi
  elif echo "${sa_error}" | grep -q "PERMISSION_DENIED"; then
    warn "起動用サービスアカウントを参照する権限がありません。存在は未確認です。"
  else
    ng "起動用サービスアカウントが存在しません: ${SCHEDULER_SA_EMAIL}"
  fi
else
  ng "Cloud Scheduler ジョブが見つからないか、参照する権限がありません。"
fi

# ---------------------------------------------------------------------------
# 直近の実行
# ---------------------------------------------------------------------------

section "直近の実行"

if executions=$(gcloud run jobs executions list \
  --job "${CLOUD_RUN_JOB_NAME}" \
  --region "${GCP_REGION}" \
  --project "${GCP_PROJECT_ID}" \
  --limit 5 \
  --sort-by="~metadata.creationTimestamp" \
  --format='value[separator="|"](metadata.name, metadata.creationTimestamp, status.succeededCount, status.failedCount, metadata.annotations."run.googleapis.com/creator")' 2>/dev/null); then

  if [ -z "${executions}" ]; then
    ng "実行履歴がありません。一度も動いていません。"
  else
    latest_result=""
    newest_scheduled_epoch=0

    while IFS='|' read -r exec_name created succeeded failed creator; do
      [ -n "${exec_name}" ] || continue

      if [ "${creator}" = "${SCHEDULER_SA_EMAIL}" ]; then
        origin="自動"
        created_epoch=$(date -d "${created}" +%s 2>/dev/null || echo 0)
        if [ "${created_epoch}" -gt "${newest_scheduled_epoch}" ]; then
          newest_scheduled_epoch="${created_epoch}"
        fi
      else
        origin="手動"
      fi

      if [ -n "${failed}" ] && [ "${failed}" != "0" ]; then
        result="失敗"
      elif [ -z "${succeeded}" ] || [ "${succeeded}" = "0" ]; then
        result="実行中または未完了"
      else
        result="成功"
      fi

      info "${created} ${origin} ${result} (${exec_name})"

      if [ -z "${latest_result}" ]; then
        latest_result="${result}"
      fi
    done <<< "${executions}"

    if [ "${latest_result}" = "失敗" ]; then
      ng "直近の実行が失敗しています。ログを確認してください。"
    fi

    # スケジューラが有効なのに自動実行の形跡がなければ、自動売買は止まっている。
    if [ "${scheduler_state}" = "ENABLED" ]; then
      stale_threshold=$(( $(date +%s) - STALE_MINUTES * 60 ))
      if [ "${newest_scheduled_epoch}" -lt "${stale_threshold}" ]; then
        ng "直近 ${STALE_MINUTES} 分間、スケジューラによる自動実行がありません。自動売買は動いていません。"
      else
        ok "スケジューラによる自動実行が動いています。"
      fi
    fi
  fi
else
  ng "実行履歴を取得できませんでした。"
fi

# ---------------------------------------------------------------------------
# まとめ
# ---------------------------------------------------------------------------

echo
echo "結果: 異常 ${error_count} 件 / 警告 ${warn_count} 件"

if [ "${error_count}" -gt 0 ]; then
  exit 1
fi
