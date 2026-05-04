## 概要

<!-- 変更の目的と背景を簡潔に記載 -->

## 変更種類（必須）

<!-- いずれかに [x] を付ける。複数選択可 -->

- [ ] feature
- [ ] fix
- [ ] refactor
- [ ] docs
- [ ] chore

## スコープ宣言（必須）

<!-- このPRが解決する「1つのユーザー価値」または「1つの失敗モード改善」を明記 -->

- 対象:
- 非対象:

## 変更内容

-

## 影響範囲

-

## 確認手順

1.
2.

## 分割方針チェック

- [ ] リファクタと機能追加を同一PRに混在させていない ([docs/ai/change-granularity.md](../docs/ai/change-granularity.md) 参照)
- [ ] `domain` 変更時に `infrastructure` を同時変更していない（必要時は別PR） ([docs/ai/kotlin-boundary-rules.md](../docs/ai/kotlin-boundary-rules.md) 参照)
- [ ] `application` にオーケストレーション以外のロジックを追加していない
