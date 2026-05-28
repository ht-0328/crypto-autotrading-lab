#!/bin/bash

# エラー発生時にスクリプトを終了する
set -e

# リポジトリのルートディレクトリを取得
REPO_ROOT=$(cd $(dirname $0)/../.. && pwd)
cd "$REPO_ROOT"

APP_DATA_DIR="$REPO_ROOT/data/local-devcontainer"
ORIGINAL_CONFIG="$REPO_ROOT/config/application-gmo.yaml"
RUNTIME_CONFIG="$APP_DATA_DIR/application-runtime.yaml"

# 出力先ディレクトリの作成
mkdir -p "$APP_DATA_DIR"

echo "実行内容を選択してください:"
echo "1) メイン実行"
echo "2) CSV取得"
echo "3) バックテスト"
read -p "> " exec_choice

case "$exec_choice" in
  1)
    echo ""
    echo "実行モードを選択してください:"
    echo "1) ドライラン"
    echo "2) 実注文"
    read -p "> " mode_choice

    case "$mode_choice" in
      1)
        dry_run=true
        real_trade_enabled=false
        mode_name="ドライラン"
        ;;
      2)
        dry_run=false
        real_trade_enabled=true
        mode_name="実注文"
        ;;
      *)
        echo "無効な選択です。終了します。"
        exit 1
        ;;
    esac

    echo ""
    echo "注文サイズを選択してください:"
    echo "1) 金額指定で買う"
    echo "2) 全買い"
    read -p "> " size_choice

    case "$size_choice" in
      1)
        order_sizing_mode="FIXED_AMOUNT"
        read -p "trade_amountを入力してください (未入力の場合は元設定の値を使用): " input_trade_amount
        ;;
      2)
        order_sizing_mode="ALL_IN"
        input_trade_amount=""
        ;;
      *)
        echo "無効な選択です。終了します。"
        exit 1
        ;;
    esac

    # 設定ファイルの生成
    cp "$ORIGINAL_CONFIG" "$RUNTIME_CONFIG"

    # OSに応じたsedコマンドの調整 (macOS対応のため)
    if sed --version >/dev/null 2>&1; then
      SED_INPLACE="sed -i"
    else
      SED_INPLACE="sed -i ''"
    fi

    # dry_runとreal_trade_enabledの書き換え
    $SED_INPLACE "s/dry_run: .*/dry_run: $dry_run/" "$RUNTIME_CONFIG"
    $SED_INPLACE "s/real_trade_enabled: .*/real_trade_enabled: $real_trade_enabled/" "$RUNTIME_CONFIG"

    # order_sizing_modeの設定
    # まずコメントアウトされているか確認し、あればアンコメントして置換、有効な設定があれば置換、なければ追加
    if grep -q "^[[:space:]]*#[[:space:]]*order_sizing_mode:" "$RUNTIME_CONFIG"; then
      $SED_INPLACE "s/^[[:space:]]*#[[:space:]]*order_sizing_mode:.*/  order_sizing_mode: \"$order_sizing_mode\"/" "$RUNTIME_CONFIG"
    elif grep -q "^[[:space:]]*order_sizing_mode:" "$RUNTIME_CONFIG"; then
      $SED_INPLACE "s/^[[:space:]]*order_sizing_mode:.*/  order_sizing_mode: \"$order_sizing_mode\"/" "$RUNTIME_CONFIG"
    else
      $SED_INPLACE "/^trading:/a\\
  order_sizing_mode: \"$order_sizing_mode\"
" "$RUNTIME_CONFIG"
    fi

    # trade_amountの書き換え (入力がある場合のみ)
    if [ -n "$input_trade_amount" ]; then
      $SED_INPLACE "s/trade_amount: .*/trade_amount: $input_trade_amount/" "$RUNTIME_CONFIG"
    else
      # 入力がない場合は元の値を使用（取得して表示用にする）
      input_trade_amount=$(grep "trade_amount:" "$RUNTIME_CONFIG" | awk '{print $2}')
    fi

    if [ "$mode_choice" = "2" ]; then
      echo ""
      echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
      echo "警告: 実注文を実行します"
      echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
      echo "使用する設定ファイル: $RUNTIME_CONFIG"
      echo "dry_run=$dry_run"
      echo "real_trade_enabled=$real_trade_enabled"
      echo "注文サイズモード: $order_sizing_mode"
      echo "trade_amount: $input_trade_amount"
      echo ""
      read -p "本当に実行しますか？ (yesと入力してください): " confirm
      if [ "$confirm" != "yes" ]; then
        echo "実行を中止しました。"
        exit 0
      fi
    fi

    export APP_CONFIG_PATH="$RUNTIME_CONFIG"
    export APP_DATA_DIR="$APP_DATA_DIR"

    cd projects/crypto-autotrading-app
    ./gradlew run
    ;;
  2)
    export APP_DATA_DIR="$APP_DATA_DIR"
    cd projects/crypto-autotrading-app
    ./gradlew exportKlinesCsv
    ;;
  3)
    export APP_DATA_DIR="$APP_DATA_DIR"
    cd projects/crypto-autotrading-app
    ./gradlew runBacktest
    ;;
  *)
    echo "無効な選択です。終了します。"
    exit 1
    ;;
esac
