#!/bin/bash

# エラー発生時にスクリプトを終了する
set -e

# リポジトリのルートディレクトリを取得
REPO_ROOT=$(cd $(dirname $0)/../.. && pwd)
cd "$REPO_ROOT"

# デフォルト環境変数のセットアップ
export APP_DATA_DIR="$REPO_ROOT/data/local-devcontainer"
export KLINE_EXPORT_OUTPUT_PATH="$APP_DATA_DIR/klines.csv"
export BACKTEST_KLINE_CSV_PATH="$KLINE_EXPORT_OUTPUT_PATH"
export BACKTEST_SUMMARY_OUTPUT_PATH="$APP_DATA_DIR/backtest-summary.csv"
export BACKTEST_STEPS_OUTPUT_PATH="$APP_DATA_DIR/backtest-steps.csv"

# 追加でデフォルト値を安全に設定（必要に応じて適宜変更）
export KLINE_EXPORT_SYMBOL=${KLINE_EXPORT_SYMBOL:-"BTC"}
export KLINE_EXPORT_INTERVAL=${KLINE_EXPORT_INTERVAL:-"5min"}
export BACKTEST_STRATEGY_NAME=${BACKTEST_STRATEGY_NAME:-"SafeReboundStrategy"}

# アプリケーション設定としてPublic APIを本物に向けるため、一時設定を生成
ORIGINAL_CONFIG="$REPO_ROOT/config/application-gmo.yaml"
RUNTIME_CONFIG="$APP_DATA_DIR/application-runtime.yaml"

# 出力先ディレクトリの作成
mkdir -p "$APP_DATA_DIR"

# 実行時設定ファイルの生成 (public_base_urlを本物に向ける)
cp "$ORIGINAL_CONFIG" "$RUNTIME_CONFIG"
if sed --version >/dev/null 2>&1; then
  SED_INPLACE="sed -i"
else
  SED_INPLACE="sed -i ''"
fi
$SED_INPLACE "s|public_base_url: .*|public_base_url: \"https://api.coin.z.com/public\"|" "$RUNTIME_CONFIG"

export APP_CONFIG_PATH="$RUNTIME_CONFIG"

echo "実行内容を選択してください:"
echo "1) リアルPublic APIでK線CSV取得"
echo "2) リアルPrivate APIで残高確認"
echo "3) 取得済みCSVでバックテスト"
echo "4) CSV取得 → 残高確認 → バックテストをまとめて実行"
read -p "> " exec_choice

# 関数: CSV取得
function run_csv_export() {
    echo "=== K線CSV取得を開始します ==="
    cd "$REPO_ROOT/projects/crypto-autotrading-app"
    ./gradlew exportKlinesCsv
    echo "=== K線CSV取得が完了しました ==="
}

# 関数: 残高確認
function run_balance_check() {
    echo "=== Private API残高確認を開始します ==="
    if [ -z "$GMO_API_KEY" ] || [ -z "$GMO_API_SECRET" ]; then
        echo "エラー: GMO_API_KEY または GMO_API_SECRET が設定されていません。"
        echo "環境変数を設定してから再度実行してください。"
        exit 1
    fi
    cd "$REPO_ROOT/projects/crypto-autotrading-app"
    ./gradlew checkPrivateApi
    echo "=== Private API残高確認が完了しました ==="
}

# 関数: JPY残高の取得と BACKTEST_INITIAL_CAPITAL への設定
function fetch_jpy_available_balance() {
    echo "=== Private API残高確認を開始します ==="
    if [ -z "$GMO_API_KEY" ] || [ -z "$GMO_API_SECRET" ]; then
        echo "エラー: GMO_API_KEY または GMO_API_SECRET が設定されていません。"
        echo "環境変数を設定してから再度実行してください。"
        exit 1
    fi
    cd "$REPO_ROOT/projects/crypto-autotrading-app"
    local output
    output=$(./gradlew checkPrivateApi -q 2>&1)

    local jpy_available
    jpy_available=$(echo "$output" | grep "JPY_AVAILABLE=" | cut -d'=' -f2 | tr -d '\r')

    if [ -z "$jpy_available" ]; then
        echo "エラー: JPY利用可能残高を取得できませんでした。"
        exit 1
    fi

    echo "JPY利用可能残高を取得しました: $jpy_available"
    export BACKTEST_INITIAL_CAPITAL="$jpy_available"
}

# 関数: バックテスト初期資金の取得元の選択
function select_backtest_initial_capital_source() {
    echo ""
    echo "バックテストの初期資金の取得元を選択してください:"
    echo "1) 設定ファイルの trading.initial_capital を使う"
    echo "2) Private APIで取得したJPY利用可能残高を使う"
    read -p "> " capital_source_choice

    if [ "$capital_source_choice" == "1" ]; then
        unset BACKTEST_INITIAL_CAPITAL
        echo "設定ファイルの trading.initial_capital を使います。"
    elif [ "$capital_source_choice" == "2" ]; then
        fetch_jpy_available_balance
    else
        echo "エラー: 初期資金の取得元の選択が不正です。"
        exit 1
    fi
}

# 関数: バックテスト
function run_backtest() {
    echo "=== バックテストを開始します ==="
    cd "$REPO_ROOT/projects/crypto-autotrading-app"
    ./gradlew runBacktest
    echo "=== バックテストが完了しました ==="
}

# 関数: 注文数量モードの選択と反映
function apply_order_sizing_mode() {
    local mode="$1"

    if grep -q "^[[:space:]]*#[[:space:]]*order_sizing_mode:" "$RUNTIME_CONFIG"; then
        $SED_INPLACE "s/^[[:space:]]*#[[:space:]]*order_sizing_mode:.*/  order_sizing_mode: \"$mode\"/" "$RUNTIME_CONFIG"
    elif grep -q "^[[:space:]]*order_sizing_mode:" "$RUNTIME_CONFIG"; then
        $SED_INPLACE "s/^[[:space:]]*order_sizing_mode:.*/  order_sizing_mode: \"$mode\"/" "$RUNTIME_CONFIG"
    else
        if sed --version >/dev/null 2>&1; then
            $SED_INPLACE "/^trading:/a\  order_sizing_mode: \"$mode\"" "$RUNTIME_CONFIG"
        else
            $SED_INPLACE "/^trading:/a\\"$'\n'"  order_sizing_mode: \"$mode\"" "$RUNTIME_CONFIG"
        fi
    fi
}

function select_order_sizing_mode() {
    echo ""
    echo "バックテストの買い方を選択してください:"
    echo "1) 金額指定"
    echo "2) 全買い"
    read -p "> " order_mode_choice

    if [ "$order_mode_choice" == "1" ]; then
        apply_order_sizing_mode "FIXED_AMOUNT"
    elif [ "$order_mode_choice" == "2" ]; then
        apply_order_sizing_mode "ALL_IN"
    else
        echo "無効な選択です。デフォルトの金額指定を使用します。"
        apply_order_sizing_mode "FIXED_AMOUNT"
    fi
}

function select_kline_export_date_range() {
    echo ""
    read -p "K線取得の開始日を入力してください（yyyyMMdd）: " input_start_date
    read -p "K線取得の終了日を入力してください（yyyyMMdd）: " input_end_date

    if ! [[ "$input_start_date" =~ ^[0-9]{8}$ ]]; then
        echo "エラー: 開始日は yyyyMMdd 形式で入力してください。"
        exit 1
    fi

    if ! [[ "$input_end_date" =~ ^[0-9]{8}$ ]]; then
        echo "エラー: 終了日は yyyyMMdd 形式で入力してください。"
        exit 1
    fi

    export KLINE_EXPORT_START_DATE="$input_start_date"
    export KLINE_EXPORT_END_DATE="$input_end_date"

    echo "K線取得期間: $KLINE_EXPORT_START_DATE 〜 $KLINE_EXPORT_END_DATE"
}

case "$exec_choice" in
  1)
    select_kline_export_date_range
    run_csv_export
    ;;
  2)
    run_balance_check
    ;;
  3)
    select_backtest_initial_capital_source
    select_order_sizing_mode
    run_backtest
    ;;
  4)
    select_kline_export_date_range
    run_csv_export
    select_backtest_initial_capital_source
    select_order_sizing_mode
    run_backtest
    ;;
  *)
    echo "無効な選択です。終了します。"
    exit 1
    ;;
esac
