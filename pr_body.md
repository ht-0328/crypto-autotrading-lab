## 概要
- Cloud Scheduler 管理用の `scheduler-gcp.yml` を追加した
- Cloud Run Job を定期実行する Scheduler Job を作成・更新できるようにした
- Scheduler Job の pause / resume / delete / run を手動実行できるようにした
- GitHub Actions の手動実行画面で分かりやすいように、action の選択肢に日本語説明を追加した

## 変更種類（必須）
- [x] feature
- [ ] fix
- [ ] refactor
- [ ] docs
- [ ] chore

## スコープ宣言（必須）
- 対象: Cloud Run Job を定期実行するための Cloud Scheduler の管理 (GitHub Actions 経由)
- 非対象: アプリケーションコードの変更、既存 CI/CD への組み込み

## 変更内容
- `.github/workflows/scheduler-gcp.yml` の新規作成
  - Scheduler 用サービスアカウント作成 (作成・更新時)
  - Scheduler Job の作成・更新・停止・再開・削除・手動実行機能の提供
  - action 入力値の内部変換 (日本語から英語へのマッピング)
- `docs/human/gcp-deployment.md` に Cloud Scheduler による定期実行の手順を追記
- `docs/human/github-actions-gcp-deploy-setup.md` に Scheduler 関連の GitHub Variables を追記

## 影響範囲
- 新規追加した GitHub Actions ワークフロー (`scheduler-gcp.yml`) およびそのドキュメントのみ。
- 既存のアプリケーション動作、CI/CD ワークフロー (`ci.yml`, `deploy-gcp.yml`) への影響はなし。

## 確認手順
1. `action=create` で Scheduler Job が作成または更新されること
2. `action=pause` で停止できること
3. `action=resume` で再開できること
4. `action=run` で手動実行できること
5. `action=delete` で削除できること

## 確認方法
- `action=create` で Scheduler Job が作成または更新されること
- `action=pause` で停止できること
- `action=resume` で再開できること
- `action=run` で手動実行できること
- `action=delete` で削除できること

## 注意点
- CI には組み込んでいない
- deploy-gcp.yml には組み込んでいない
- Scheduler Job を作成すると、設定した cron に従って Cloud Run Job が定期実行される
- 料金を止めたい場合は pause または delete を実行する必要がある
- Cloud Scheduler Job を作成した場合、料金や定期実行を止めるには Scheduler Job の削除または pause が必要
- cleanup workflow 側にも Scheduler Job 削除処理を追加予定

## 使用する GitHub Variables
以下は作成済みです。
- `CLOUD_SCHEDULER_JOB_NAME`
- `SCHEDULER_SERVICE_ACCOUNT_NAME`
- `SCHEDULER_CRON`
- `SCHEDULER_TIME_ZONE`

## 分割方針チェック
- [x] リファクタと機能追加を同一PRに混在させていない
- [x] `domain` 変更時に `infrastructure` を同時変更していない（必要時は別PR）
- [x] `application` にオーケストレーション以外のロジックを追加していない
