# AI向けドキュメント一覧

## 概要

このディレクトリ（`docs/ai/`）には、プロジェクトに参加するAIエージェント（Jules, Codex 等）向けの制約、ルール、期待される挙動、および特定のスキル定義が格納されています。

## AIエージェントが最初に読む順番

タスクに着手する前に、AIエージェントは必ず以下の順番でドキュメントを読み込んでください。

1. [AGENTS.md](../../AGENTS.md) (プロジェクト全体のコアルール)
2. [`agents-guidelines.md`](agents-guidelines.md) (詳細な行動規範と禁止事項)
3. [`prompt-template.md`](prompt-template.md) (出力フォーマット)

## 作業内容ごとの参照先

タスクの種類に応じて、追加で以下のドキュメントを参照してください。

* **PR作成やコードレビューを行う場合**:
  * [`review-checklist.md`](review-checklist.md)
  * [`change-granularity.md`](change-granularity.md)
* **Kotlinのバックエンドコード（アーキテクチャ境界）を変更する場合**:
  * [`kotlin-boundary-rules.md`](kotlin-boundary-rules.md)
* **特定領域の深い分析や要件定義を求められた場合**:
  * [`skills-catalog.md`](skills-catalog.md) (必要なスキルを特定し、`docs/ai/skills/` 内の該当スキル定義を読み込む)

## 各AI向けドキュメントの役割

| ドキュメント | 役割 |
|---|---|
| [`agents-guidelines.md`](agents-guidelines.md) | 絶対禁止事項や実行必須のチェック事項を定義するベースラインルール |
| [`prompt-template.md`](prompt-template.md) | ユーザーからの依頼の解釈方法と、作業完了時の必須出力フォーマット |
| [`review-checklist.md`](review-checklist.md) | コード変更時に厳守すべき品質基準、レイヤ制約、監視、例外処理のチェックリスト |
| [`change-granularity.md`](change-granularity.md) | PRの粒度（1PR=1価値）、リファクタリングと機能追加の分離ルール |
| [`kotlin-boundary-rules.md`](kotlin-boundary-rules.md) | Domain, Application, Infrastructure各層の責務と境界の定義 |
| [`skills-catalog.md`](skills-catalog.md) | AIが提供可能な専門スキルの一覧と、その適用条件・入出力定義 |
