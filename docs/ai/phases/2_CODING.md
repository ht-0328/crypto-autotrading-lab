# Phase 2: 実装と自己修復 (Coding & Testing)

## 絶対遵守事項 (Critical Rules)
- **セキュリティ**: 情報漏洩を防ぐため、APIキー、シークレット、トークン等は絶対に生成・ログ出力・コミットしないでください。
- **言語**: ユーザーとの円滑なコミュニケーションのため、コード内のコメントや説明はすべて**日本語**で記述してください。

このフェーズの目的は、Phase 1の計画に基づきコードを修正し、テストをパスさせることです。
以下の【Thought (思考) → Action (行動) → Observation (観察)】のループ（ReActパターン）に倣って、自律的にエラーを修復してください。

## ReActループと自己修復の具体例 (Few-Shot)
**Thought (思考)**: 修正方針が固まったのでコードを編集する。
**Action (行動)**: 対象ファイルを編集。
**Observation (観察)**: [編集成功]
**Thought (思考)**: 修正が終わったので、検証のためにテストとビルドを実行する。
**Action (行動)**: `./gradlew test build` を実行。
**Observation (観察)**: [コンパイルエラーが発生しました: Type mismatch...]
**Thought (思考)**: エラーが発生した。ログを観察すると型の不一致が原因のようだ。コードを再修正し、再度テストを実行する。
**Action (行動)**: コードを再修正。
**Observation (観察)**: [編集成功]
**Thought (思考)**: 再テストを実行する。
**Action (行動)**: `./gradlew test build` を実行。
**Observation (観察)**: [BUILD SUCCESSFUL]
**Thought (思考)**: テストとビルドが成功した。AGENTS.md の指示に従い、Phase 3 (`docs/ai/phases/3_REPORTING.md`) を読み込んで報告に進む。
