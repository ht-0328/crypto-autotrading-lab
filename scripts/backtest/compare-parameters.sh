#!/bin/bash
set -euo pipefail

# 売買パラメータの値を変えてバックテストを繰り返し、結果を1つの表にまとめる。
#
# 「閾値を緩めれば買う回数は増えるが、勝率と利益率は下がる」といったトレードオフを、
# 設定を変える前に数字で比べるための道具。実運用の設定は変更しない。
#
# 使い方（ローカル）:
#   SWEEP_VALUES="0.003 0.005 0.007" \
#   BACKTEST_KLINE_CSV_PATH=data/local-devcontainer/klines.csv \
#   scripts/backtest/compare-parameters.sh
#
# 環境変数:
#   SWEEP_PARAMETER          変化させる環境変数名（既定: TRADING_BUY_THRESHOLD）
#   SWEEP_VALUES             試す値。空白区切り（必須）
#   BACKTEST_KLINE_CSV_PATH  入力の過去K線CSV（必須）
#   BACKTEST_STRATEGY_NAME   戦略名（既定: SafeReboundStrategy）
#   BACKTEST_INITIAL_CAPITAL 初期資金（既定: 10000）
#   BACKTEST_CONFIG_PATH     比較の土台にする設定ファイル
#                            （既定: <リポジトリ>/config/application-gmo.yaml）
#   OUTPUT_DIR               出力先（既定: <リポジトリ>/data/backtest/compare）
#
# 終了コード: 0 = 全ての値で成功 / 1 = 入力不正、または1つでも失敗

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(git -C "${SCRIPT_DIR}" rev-parse --show-toplevel)"
APP_DIR="${REPO_ROOT}/projects/crypto-autotrading-app"

SWEEP_PARAMETER="${SWEEP_PARAMETER:-TRADING_BUY_THRESHOLD}"
SWEEP_VALUES="${SWEEP_VALUES:-}"
BACKTEST_KLINE_CSV_PATH="${BACKTEST_KLINE_CSV_PATH:-}"
BACKTEST_STRATEGY_NAME="${BACKTEST_STRATEGY_NAME:-SafeReboundStrategy}"
BACKTEST_INITIAL_CAPITAL="${BACKTEST_INITIAL_CAPITAL:-10000}"
OUTPUT_DIR="${OUTPUT_DIR:-${REPO_ROOT}/data/backtest/compare}"
# 比較の土台になる設定が実行環境によって変わると、結果を比べられない。
# 開発環境では APP_CONFIG_PATH が別の設定を指していることがあるため、
# それを引き継がず、この変数で明示的に決める。既定は本番と同じ設定ファイル。
BACKTEST_CONFIG_PATH="${BACKTEST_CONFIG_PATH:-${REPO_ROOT}/config/application-gmo.yaml}"

# 設定として読まれない名前を指定すると、全ての実行が同じ条件になり、
# 「差が出なかった」という誤った結論に静かに到達する。読まれる名前だけを許可する。
ALLOWED_PARAMETERS="
TRADING_BUY_THRESHOLD
TRADING_SELL_THRESHOLD
TRADING_VOLATILITY_THRESHOLD
TRADING_SHARP_CHANGE_THRESHOLD
TRADING_TRADE_AMOUNT
TRADING_COOLDOWN_LENGTH
TRADING_ATR_LENGTH
TRADING_ATR_PROFIT_MULTIPLIER
TRADING_ATR_LOSS_MULTIPLIER
"

if ! echo "${ALLOWED_PARAMETERS}" | grep -qx "${SWEEP_PARAMETER}"; then
  echo "エラー: SWEEP_PARAMETER に指定できない名前です: ${SWEEP_PARAMETER}" >&2
  echo "指定できるのは次のいずれかです:" >&2
  echo "${ALLOWED_PARAMETERS}" | sed '/^$/d;s/^/  /' >&2
  exit 1
fi

if [ -z "${SWEEP_VALUES}" ]; then
  echo "エラー: SWEEP_VALUES が未設定です（例: SWEEP_VALUES=\"0.003 0.005 0.007\"）" >&2
  exit 1
fi

if [ -z "${BACKTEST_KLINE_CSV_PATH}" ]; then
  echo "エラー: BACKTEST_KLINE_CSV_PATH が未設定です" >&2
  exit 1
fi

# 実行時の作業ディレクトリが Gradle プロジェクトへ移るため、相対パスのままでは解決できない。
case "${BACKTEST_KLINE_CSV_PATH}" in
  /*) KLINE_CSV_ABS="${BACKTEST_KLINE_CSV_PATH}" ;;
  *)  KLINE_CSV_ABS="$(pwd)/${BACKTEST_KLINE_CSV_PATH}" ;;
esac

if [ ! -f "${KLINE_CSV_ABS}" ]; then
  echo "エラー: 過去K線CSVが見つかりません: ${KLINE_CSV_ABS}" >&2
  exit 1
fi

if [ ! -f "${BACKTEST_CONFIG_PATH}" ]; then
  echo "エラー: 設定ファイルが見つかりません: ${BACKTEST_CONFIG_PATH}" >&2
  exit 1
fi

mkdir -p "${OUTPUT_DIR}"

kline_rows=$(($(wc -l < "${KLINE_CSV_ABS}") - 1))

echo "比較するパラメータ: ${SWEEP_PARAMETER}"
echo "試す値            : ${SWEEP_VALUES}"
echo "戦略              : ${BACKTEST_STRATEGY_NAME}"
echo "初期資金          : ${BACKTEST_INITIAL_CAPITAL}"
echo "過去K線CSV        : ${KLINE_CSV_ABS}（${kline_rows} 本）"
echo "設定ファイル      : ${BACKTEST_CONFIG_PATH}"
echo "出力先            : ${OUTPUT_DIR}"
echo

failed=0
summary_paths=""

for value in ${SWEEP_VALUES}; do
  # ファイル名に使えるようにドットを置き換える
  safe_value="${value//./_}"
  summary_path="${OUTPUT_DIR}/summary_${SWEEP_PARAMETER}_${safe_value}.csv"
  steps_path="${OUTPUT_DIR}/steps_${SWEEP_PARAMETER}_${safe_value}.csv"
  log_path="${OUTPUT_DIR}/gradle_${SWEEP_PARAMETER}_${safe_value}.log"

  echo "実行中: ${SWEEP_PARAMETER}=${value}"

  # Gradle の出力は普段は不要なので伏せ、失敗したときだけ見せる。
  if (
    cd "${APP_DIR}"
    env "${SWEEP_PARAMETER}=${value}" \
      APP_CONFIG_PATH="${BACKTEST_CONFIG_PATH}" \
      BACKTEST_KLINE_CSV_PATH="${KLINE_CSV_ABS}" \
      BACKTEST_STRATEGY_NAME="${BACKTEST_STRATEGY_NAME}" \
      BACKTEST_INITIAL_CAPITAL="${BACKTEST_INITIAL_CAPITAL}" \
      BACKTEST_SUMMARY_OUTPUT_PATH="${summary_path}" \
      BACKTEST_STEPS_OUTPUT_PATH="${steps_path}" \
      ./gradlew runBacktest --console=plain --quiet
  ) > "${log_path}" 2>&1; then
    summary_paths="${summary_paths} ${value}=${summary_path}"
  else
    echo "  失敗しました。ログ: ${log_path}"
    tail -n 20 "${log_path}" | sed 's/^/    /'
    failed=1
  fi
done

if [ -z "${summary_paths}" ]; then
  echo >&2
  echo "エラー: 成功した実行がありません。" >&2
  exit 1
fi

# ---------------------------------------------------------------------------
# 結果の集計
# ---------------------------------------------------------------------------

comparison_path="${OUTPUT_DIR}/comparison.md"

SWEEP_PARAMETER="${SWEEP_PARAMETER}" \
BACKTEST_STRATEGY_NAME="${BACKTEST_STRATEGY_NAME}" \
BACKTEST_INITIAL_CAPITAL="${BACKTEST_INITIAL_CAPITAL}" \
KLINE_ROWS="${kline_rows}" \
BACKTEST_CONFIG_PATH="${BACKTEST_CONFIG_PATH}" \
SUMMARY_PATHS="${summary_paths}" \
python3 - "${comparison_path}" <<'PY'
import csv
import os
import sys

out_path = sys.argv[1]
parameter = os.environ["SWEEP_PARAMETER"]
rows = []

for entry in os.environ["SUMMARY_PATHS"].split():
    value, path = entry.split("=", 1)
    with open(path, encoding="utf-8") as f:
        record = next(csv.DictReader(f))
    rows.append((value, record))


def num(record, key, digits=2, percent=False, sign=False):
    """CSV の文字列を読みやすい桁数にそろえる。値が無い場合は - を返す。"""
    raw = record.get(key, "")
    if raw in ("", None):
        return "-"
    v = float(raw) * (100 if percent else 1)
    body = f"{v:,.{digits}f}"
    if sign and v > 0:
        body = "+" + body
    return body + ("%" if percent else "")


lines = [
    f"### {parameter} を変えた場合の比較",
    "",
    f"- 戦略: {os.environ['BACKTEST_STRATEGY_NAME']}",
    f"- 初期資金: {int(os.environ['BACKTEST_INITIAL_CAPITAL']):,} 円",
    f"- 対象データ: 5分足 {int(os.environ['KLINE_ROWS']):,} 本（約 {int(os.environ['KLINE_ROWS']) * 5 / 60 / 24:.0f} 日）",
    f"- 設定ファイル: `{os.path.basename(os.environ['BACKTEST_CONFIG_PATH'])}`（比較する値以外はここの値を使う）",
    "",
    f"| {parameter} | 確定損益 | 利益率 | 買い回数 | 勝率 | 最大ドローダウン | 最大連続損切り | 未決済 |",
    "| --- | ---: | ---: | ---: | ---: | ---: | ---: | --- |",
]

for value, r in rows:
    lines.append(
        f"| {value} "
        f"| {num(r, '確定損益', 0, sign=True)} 円 "
        f"| {num(r, '利益率', 2, percent=True, sign=True)} "
        f"| {num(r, '買い回数', 0)} "
        f"| {num(r, '勝率', 1, percent=True)} "
        f"| {num(r, '最大ドローダウン', 1, percent=True)} "
        f"| {num(r, '最大連続損切り回数', 0)} "
        f"| {r.get('未決済ポジションあり', '-')} |"
    )

lines += [
    "",
    "利益率だけで選ばないこと。最大ドローダウンと最大連続損切りは、途中でどれだけ含み損に",
    "耐える必要があるかを表す。買い回数が極端に少ない値は、たまたま当たっただけの可能性がある。",
    "",
    "**このバックテストは実注文の安全ルールを含まない。** 1日の注文上限、日次損失による停止、",
    "連敗による停止は本番だけで効くため、本番の売買回数はここより少なくなる。",
]

text = "\n".join(lines) + "\n"
open(out_path, "w", encoding="utf-8").write(text)
print(text)
PY

echo "比較表: ${comparison_path}"

if [ "${failed}" -ne 0 ]; then
  echo "警告: 一部の値で失敗しました。上の表は成功した分だけです。" >&2
  exit 1
fi
