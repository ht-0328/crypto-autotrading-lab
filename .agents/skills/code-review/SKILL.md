---
name: code-review
description: >-
  crypto-autotrading-lab の Kotlin コード、設定、Gradle、CI の変更をレビューするチェックリスト。
  実装ルール、レイヤ依存、後方互換性、ログと秘密情報、例外処理、テストを確認する。
  コード変更を終える前の自己点検、PRのレビュー、レビュー指摘を書くときに使用する。
---

# コードレビュー チェックリスト

自分の変更を提出する前と、他者のPRをレビューするときの両方で使います。
指摘するときは、必ず「どのファイルのどこが」「なぜ問題か」「どう直すか」をセットで書いてください。

## 1. Kotlin 実装ルール

判断基準の詳細は [kotlin-code-rules](../kotlin-code-rules/SKILL.md) を参照してください。

- [ ] 1ファイルに `data class` が複数定義されていないか。
- [ ] `private` を含むすべての関数にKDocがあるか。
- [ ] KDocに必要な `@param` / `@return` / `@property` が書かれているか。KDocの内容が実際の処理と一致しているか。
- [ ] 設定値・初期資金・注文金額などに、安易なデフォルト値が入っていないか。
- [ ] 売買判定が Scope functions のチェーンに隠れていないか。重要な副作用が `also` / `run` の中に隠れていないか。

## 2. レイヤ依存

ルールの詳細は [kotlin-layer-boundaries](../kotlin-layer-boundaries/SKILL.md) を参照してください。

- [ ] `domain` に外部I/O（HTTP、ファイル、DB、環境変数、現在時刻の直接取得）が入っていないか。
- [ ] `domain` が `application` / `infrastructure` / `presentation` に依存していないか。
- [ ] `infrastructure` のレスポンス型が `domain` の interface に漏れていないか。
- [ ] `application` にビジネスロジック（閾値判定・売買判断）が混入していないか。
- [ ] 新しい境界ルールを追加した場合、`ArchitectureTest` に検査が追加されているか。

## 3. 設定と互換性

- [ ] `config/application-*.yaml` の既存キーや意味を壊していないか。
- [ ] 新しく追加した設定値が、未定義の環境で安全にフォールバックするか、または明示的に失敗するか。
- [ ] GMO API / WireMock / ローカル Docker の環境差を考慮した実装になっているか。
- [ ] 公開API（`domain` の interface、`presentation` のエントリポイント、設定キー）の後方互換性を壊していないか。壊す場合、その影響と移行手順がPR本文に書かれているか。

## 4. ログと監視

- [ ] 追跡に必要な重要イベント（起動、判定結果、保存、エラー）が適切なログレベルで出ているか。
- [ ] エラーログに原因究明に足る文脈（例外種別、マスク済みの識別子など安全な範囲の入力情報）が含まれているか。生の認証情報や注文パラメータをそのまま出していないか。
- [ ] **APIキー・シークレット等の秘密情報がログに出ていないか（マスクされているか）。**
- [ ] **GMO Private API のレスポンス本文（口座残高、保有数量、注文内容）をログや例外メッセージに含めていないか。** 既定のログレベル(info)で出してよいのは、パス・HTTPステータス・GMO のエラーコードまで。本文が必要な場合は `logger.debug` に限定する。
- [ ] `state.json` の内容（`realTrading.latestOrder` の orderId など）を、例外メッセージやエラーログにそのまま含めていないか。

## 5. 異常系と例外処理

- [ ] 例外の握りつぶし（空の `catch` など）がないか。
- [ ] リトライ処理に、無限ループを防ぐ中断条件があるか。
- [ ] 失敗時にデータや状態が不整合なまま残らないか（中途半端なファイル保存など）。
- [ ] 実資金・実注文につながる経路で、安全側に倒す判断になっているか。詳細は [trading-safety-review](../trading-safety-review/SKILL.md) を参照。

## 6. 検証

- [ ] `cd projects/crypto-autotrading-app && ./gradlew build` が通っているか。
- [ ] 変更した振る舞いに対応するテストが追加・更新されているか。
- [ ] `ArchitectureTest` やビルド設定を変更した場合、CI（[.github/workflows/ci.yml](../../../.github/workflows/ci.yml)）で引き続き実行されるか。
- [ ] PRの粒度とPR本文の形式が [pr-and-commit](../pr-and-commit/SKILL.md) に沿っているか。
