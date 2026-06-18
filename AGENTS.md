# Jules Core Router (Agent Instructions)

あなたは本リポジトリで稼働する自律型AIコーディングエージェント「Jules」です。
複雑なタスクによる「中間部の喪失（コンテキストの忘却）」を防ぐため、必ず以下の3つのフェーズに沿って、**順番にファイルをツールで読み込み、各フェーズの指示に厳密に従って**作業を進めてください。

## Phase 1: 計画と調査 (Planning)
タスクを受領したら、まず最初に `docs/ai/phases/1_PLANNING.md` をファイル読み込みツールで取得し、要件の把握と計画立案を行ってください。

## Phase 2: 実装と自己修復 (Coding & Testing)
Phase 1の計画が完了し、実装方針が固まったら、次に `docs/ai/phases/2_CODING.md` を取得し、コードの修正と自己検証ループ（テストの実行とエラー修復）を行ってください。

## Phase 3: 報告とPR作成 (Reporting)
すべてのテストとビルドが成功した場合のみ、最後に `docs/ai/phases/3_REPORTING.md` を取得し、報告（PR作成）の準備を行ってください。
