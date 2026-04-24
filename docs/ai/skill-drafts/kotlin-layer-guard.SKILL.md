# Skill: kotlin-layer-guard

## Purpose
Kotlinコードのレイヤ依存ルール違反を検出し、修正方針を提示する。

## When to use
- アーキテクチャ健全性レビュー時
- モジュール分割や依存整理の前後

## Inputs
- Kotlinソース（`projects/crypto-autotrading-app`）
- レイヤ定義（Domain/Application/Infra/UIなど）
- build設定

## Workflow
1. レイヤと許可依存方向を明文化。
2. import/型参照/Gradle依存を点検。
3. 違反箇所を重大度で分類。
4. 修正案と自動検査案（detekt/ArchUnit等）を提示。

## Output
- 違反一覧（ファイル、行、理由）
- 修正提案
- 再発防止チェック案

## Done criteria
- 依存違反の根拠と再現手順が明確。
