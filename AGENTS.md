# AGENTS.md

このリポジトリで作業するJulesやCodexを含む、すべてのAIコーディングエージェント向けの共通ルールです。
AIコーディングエージェントが作業を行う場合、この内容を前提として進めてください。

## 基本方針
- 回答、説明、コミットメッセージ、PRタイトル、PR本文、README、docs、コメントは日本語で書く。
- `docs/ai` ディレクトリはAI依頼・レビュー補助資料として参照すること。
- `docs/process` ディレクトリは開発プロセス・責務分担ルールとして参照すること。

## プロジェクト構成
- 主なアプリケーションコードは `projects/crypto-autotrading-app` に配置されている。
- 設定ファイルは `config/application-gmo.yaml`、`config/application-wiremock.yaml` に配置されている。

## Kotlin / Gradle 方針
- Kotlin、Gradle、Java、および主要ライブラリのバージョンを勝手に変更しないこと。
- 新たに依存ライブラリを追加する場合は、その理由と影響範囲をPR本文に明記すること。

## 自動売買アプリとしての安全ルール
- 売買条件、注文処理、本番API呼び出しに関わる変更は特に慎重に行うこと。
- APIキーやシークレットなどの機密情報をログに出力しないこと。

## リファクタリング方針
- 既存機能の動作を変えない整理を優先し、勝手に大規模なリファクタリングを行わないこと。
- 責務分離・重複削減・可読性向上は小さな差分で行うこと。

## 実行・テスト
変更後は必ず以下のコマンドで実行およびテストの確認を行うこと。

テストを実行する場合：
```bash
cd projects/crypto-autotrading-app
./gradlew test
```

ビルドの確認が必要な場合：
```bash
cd projects/crypto-autotrading-app
./gradlew build
```

アプリ起動に関わる変更の確認が必要な場合：
```bash
cd projects/crypto-autotrading-app
./gradlew run
```

設定ファイルを明示してアプリを起動する場合：
```bash
cd projects/crypto-autotrading-app
APP_CONFIG_PATH=../../config/application-wiremock.yaml ./gradlew run
```

## PR作成ルール
PRを作成する場合は以下のルールを守ること。
- PRタイトルとPR本文は日本語で書くこと。
- PR本文は `.github/pull_request_template.md` の形式に従うこと。
- テストを実行できなかった場合は、その理由を書くこと。
- 実行確認なしに「動作確認済み」と書かないこと。
- 変更の影響範囲と未確認事項を明記すること。

## 変更範囲のルール
- `domain`、`application`、`infrastructure` の各レイヤーの責務を混ぜないこと。
- 無関係な整形、リファクタリング、依存関係の追加を同じPRに混ぜないこと。

## 禁止事項
- APIキー、シークレット、トークン、個人情報、および `.env` ファイルを絶対にコミットしないこと。
