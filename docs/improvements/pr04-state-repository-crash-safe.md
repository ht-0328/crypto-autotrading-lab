# PR04: 状態保存をクラッシュセーフにする

| 項目 | 内容 |
| --- | --- |
| 想定読者 | この改善を実施する開発者、AIコーディングエージェント |
| 読んだあとできること | 状態保存を原子的にし、保存失敗を検知できるようにできる |
| 状態 | 実施済み（ブランチ `fix/state-repository-crash-safe`） |
| 機密区分 | 公開可 |
| 作成者 | リポジトリ管理者 |
| 保守責任者 | リポジトリ管理者 |
| 最終確認日 | 2026-08-30 |


## 対象の指摘

[findings.md](findings.md) の **I** / **J**（いずれも重要度: 高）

## なぜ直すか

[StateRepository.kt](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/infrastructure/output/StateRepository.kt) に2つの問題があります。

- **I**: `save()` が例外を握り潰してログを出すだけで、呼び出し元に伝えません。保存に失敗してもアプリは正常終了（exit 0）します。[phase1-simulation.md](../specifications/phase1-simulation.md) のエラー仕様「CSVまたはJSONファイルの保存に失敗した → エラーをログに記録し終了する」に反します。実注文と組み合わさると「注文したのに state が残らない＝二重注文」につながります。
- **J**: `file.writeText(content)` で既存ファイルに直接書いています。書き込み途中でプロセスが落ちると空ファイルや途中までの JSON が残り、次回の `load()` がデコード例外を投げて以降の実行がすべて止まります。GCS FUSE マウント上ではさらにリスクが高くなります。

## 変更対象

| ファイル | 変更内容 |
| --- | --- |
| [StateRepository.kt](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/infrastructure/output/StateRepository.kt) | `save()` の例外を再 throw。一時ファイル＋原子的 rename で保存 |
| [StateRepositoryTest.kt](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/test/kotlin/cryptoautotrading/infrastructure/output/StateRepositoryTest.kt) | 追加ケース |

## 実施手順

1. `save()` の `catch` ブロックで、ログ出力後に例外を再 throw する。

   ```kotlin
   } catch (e: Exception) {
       logger.error(e) { "状態ファイルの保存に失敗しました。パス: $stateFilePath" }
       throw e
   }
   ```

   例外メッセージに `state` の中身を入れないこと。着手前の実装は `保存しようとした状態: $state` を出力しており、実注文の orderId まで残る。

2. 書き込みを「同一ディレクトリの一時ファイルに書く → 原子的に置き換える」に変更する。

   ```kotlin
   val tempFile = File(file.parentFile, "${file.name}.tmp")
   tempFile.writeText(content)
   Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE)
   ```

   一時ファイルは必ず**同じディレクトリ**に作ること。別ファイルシステムだと `ATOMIC_MOVE` が失敗します。

3. `ATOMIC_MOVE` が使えない環境（GCS FUSE 等）のフォールバックを用意する。`AtomicMoveNotSupportedException` を捕捉し、`REPLACE_EXISTING` での move に切り替える。フォールバック時は警告ログを出す。

4. 失敗時に一時ファイルが残らないよう、`finally` で後始末する。

5. テストを追加する。
   - 保存に失敗したとき例外が呼び出し元に伝わること
   - 保存後に一時ファイル（`.tmp`）が残っていないこと
   - 既存ファイルがある状態で保存すると中身が置き換わること
   - `ATOMIC_MOVE` 非対応時にフォールバックが動くこと（`Files` をモックするか、フォールバック経路を internal 関数に切り出して直接呼ぶ）

## 受け入れ条件

- [ ] `save()` が失敗したとき例外が呼び出し元に伝わること
- [ ] 保存が一時ファイル経由で行われ、成功後に `.tmp` が残らないこと
- [ ] `ATOMIC_MOVE` 非対応環境でも保存できること
- [ ] ログと例外メッセージに `state` の中身（orderId・残高など）が含まれないこと
- [ ] 既存テストがすべて通ること

## 検証

```bash
cd projects/crypto-autotrading-app
./gradlew build
```

## 補足: 呼び出し元への影響

`save()` が例外を投げるようになると、[run()](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/application/TradingApplication.kt) の `catch` を経由して [Main.kt](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/presentation/Main.kt) まで伝播し、プロセスが異常終了します。これは仕様どおりの動作です。Cloud Run Job は `max_retries = 0` なので再実行はされません。

## スコープ外

- 注文 POST と状態保存の順序変更（[PR05](pr05-phase1-real-order-guard.md)）
- CSV 出力（`CsvRepository`）の同様の対応。今回は state.json に絞る
