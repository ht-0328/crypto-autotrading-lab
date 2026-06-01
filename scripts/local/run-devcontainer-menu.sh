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
# GNU dateとBSD dateの差異を吸収して1日前の日付を取得
if date --version >/dev/null 2>&1; then
  DEFAULT_START_DATE=$(date -u -d '1 day ago' '+%Y%m%d')
else
  DEFAULT_START_DATE=$(date -u -v-1d '+%Y%m%d')
fi
export KLINE_EXPORT_START_DATE=${KLINE_EXPORT_START_DATE:-$DEFAULT_START_DATE}
export KLINE_EXPORT_END_DATE=${KLINE_EXPORT_END_DATE:-$(date -u '+%Y%m%d')}
export BACKTEST_STRATEGY_NAME=${BACKTEST_STRATEGY_NAME:-"SafeReboundStrategy"}
export BACKTEST_INITIAL_CAPITAL=${BACKTEST_INITIAL_CAPITAL:-100000}

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

# 関数: バックテスト
function run_backtest() {
    echo "=== バックテストを開始します ==="
    cd "$REPO_ROOT/projects/crypto-autotrading-app"
    ./gradlew runBacktest
    echo "=== バックテストが完了しました ==="
}

# 関数: 注文数量モードの選択
function select_order_sizing_mode() {
    echo ""
    echo "バックテストの買い方を選択してください:"
    echo "1) 金額指定"
    echo "2) 全買い"
    read -p "> " order_mode_choice
}

case "$exec_choice" in
  1)
    run_csv_export
    ;;
  2)
    run_balance_check
    ;;
  3)
    select_order_sizing_mode
    if [ "$order_mode_choice" == "1" ]; then
        $SED_INPLACE "s/order_sizing_mode: .*/order_sizing_mode: \"FIXED_AMOUNT\"/" "$RUNTIME_CONFIG"
        # もし order_sizing_mode の設定行が存在しない場合のフォールバック（追加）
        if ! grep -q "order_sizing_mode:" "$RUNTIME_CONFIG"; then
            if sed --version >/dev/null 2>&1; then
                $SED_INPLACE "/trading:/a\  order_sizing_mode: \"FIXED_AMOUNT\"" "$RUNTIME_CONFIG"
            else
                $SED_INPLACE "/trading:/a\\"$'\n'"  order_sizing_mode: \"FIXED_AMOUNT\"" "$RUNTIME_CONFIG"
            fi
        fi
    elif [ "$order_mode_choice" == "2" ]; then
        $SED_INPLACE "s/order_sizing_mode: .*/order_sizing_mode: \"ALL_IN\"/" "$RUNTIME_CONFIG"
        if ! grep -q "order_sizing_mode:" "$RUNTIME_CONFIG"; then
            if sed --version >/dev/null 2>&1; then
                $SED_INPLACE "/trading:/a\  order_sizing_mode: \"ALL_IN\"" "$RUNTIME_CONFIG"
            else
                $SED_INPLACE "/trading:/a\\"$'\n'"  order_sizing_mode: \"ALL_IN\"" "$RUNTIME_CONFIG"
            fi
        fi
    fi
    run_backtest
    ;;
  4)
    run_csv_export

    # APIキーが設定されていれば残高確認を実行
    if [ -n "$GMO_API_KEY" ] && [ -n "$GMO_API_SECRET" ]; then
        run_balance_check
    else
        echo "GMO_API_KEY または GMO_API_SECRET が設定されていないため、残高確認をスキップします。"
    fi

    echo ""
    read -p "Private APIで確認したJPY残高を入力してください (未入力の場合はデフォルト $BACKTEST_INITIAL_CAPITAL を使用): " input_capital
    if [ -n "$input_capital" ]; then
        export BACKTEST_INITIAL_CAPITAL="$input_capital"
    fi

    select_order_sizing_mode
    if [ "$order_mode_choice" == "1" ]; then
        $SED_INPLACE "s/order_sizing_mode: .*/order_sizing_mode: \"FIXED_AMOUNT\"/" "$RUNTIME_CONFIG"
        if ! grep -q "order_sizing_mode:" "$RUNTIME_CONFIG"; then
            if sed --version >/dev/null 2>&1; then
                $SED_INPLACE "/trading:/a\  order_sizing_mode: \"FIXED_AMOUNT\"" "$RUNTIME_CONFIG"
            else
                $SED_INPLACE "/trading:/a\\"$'\n'"  order_sizing_mode: \"FIXED_AMOUNT\"" "$RUNTIME_CONFIG"
            fi
        fi
    elif [ "$order_mode_choice" == "2" ]; then
        $SED_INPLACE "s/order_sizing_mode: .*/order_sizing_mode: \"ALL_IN\"/" "$RUNTIME_CONFIG"
        if ! grep -q "order_sizing_mode:" "$RUNTIME_CONFIG"; then
            if sed --version >/dev/null 2>&1; then
                $SED_INPLACE "/trading:/a\  order_sizing_mode: \"ALL_IN\"" "$RUNTIME_CONFIG"
            else
                $SED_INPLACE "/trading:/a\\"$'\n'"  order_sizing_mode: \"ALL_IN\"" "$RUNTIME_CONFIG"
            fi
        fi
    fi

    run_backtest
    ;;
  *)
    echo "無効な選択です。終了します。"
    exit 1
    ;;
esac
