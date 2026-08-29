---
name: kotlin-code-rules
description: >-
  crypto-autotrading-lab の Kotlin コードを書く・直す・レビューするときの実装ルール。
  Scope functions と拡張関数の使い分け、命名、KDoc、data class の配置、
  お金に関わる値のデフォルト値禁止、売買ロジック特有の注意点を扱う。
  projects/crypto-autotrading-app の .kt ファイルを追加・変更するとき、
  および Kotlin の可読性をレビューするときに使用する。
---

# Kotlin 実装ルール

## 1. 目的

Kotlinコードを、読みやすさ・安全性・保守性を優先して実装・レビューするためのルールです。目的は「Kotlinらしい構文を避けること」ではなく、「読み手が自然に理解できるコードにすること」です。

## 2. 基本方針

- **Scope functions (`let`, `run`, `with`, `apply`, `also`) は禁止しません。**
- **拡張関数も禁止しません。**
- むしろ、Kotlinらしく読みやすくなり、コードの意図が直感的になる場合は**積極的に使ってください**。
- 「使わない」「乱用しない」という消極的な理由ではなく、「どうすれば最も読みやすくなるか」を基準に判断します。

## 3. Scope functions の使用方針

### 共通方針

- コードの意図が明確になる場合は積極的に使います。
- 一時変数を増やすより処理の流れが自然になるなら使ってよいです。
- ただし、`it` / `this` が分かりにくい場合は明示的な変数名を使います。
- スコープ関数のネストで読みづらくなる場合は、通常の関数や変数に分けます。

### `let`

- nullチェックと後続処理を自然につなげられる場合に使います。
- 値を別の値へ短く変換する場合に使います。
- ただし、複雑な売買判定を `let` チェーンに隠さないようにします。

#### `let` のよい例

```kotlin
val signal = marketPrice
    ?.let { calculateSignal(it) }
    ?: TradingSignal.Hold
```

#### `it` が分かりにくい場合の改善例

```kotlin
val currentPrice = price ?: return TradingSignal.Hold
val previousPrice = previous ?: return TradingSignal.Hold

return calculateSomething(currentPrice, previousPrice)
```

### `apply`

- オブジェクトの初期設定が読みやすくなる場合に使います。
- 設定値をまとめて代入する場合に使います。
- ただし、ビジネスロジックの判定や外部I/Oを隠さないようにします。

### `also`

- ログ出力や補助的な確認処理を自然に添えられる場合に使います。
- ただし、注文実行、保存、送信などの重要な副作用を `also` の中に隠さないようにします。

### `run`

- 複数の値から1つの結果を作る小さな式として読みやすい場合に使います。
- ただし、戻り値が分かりにくい場合は通常の関数に分けます。

### `with`

- 1つのオブジェクトに対して複数の関連操作を行う場合に使います。
- ただし、`this` が何を指すか分かりにくい場合は使わないようにします。

## 4. 拡張関数の使用方針

- 対象の型に自然な振る舞いを追加できる場合は積極的に使います。
- ドメインの言葉として意味が明確になる場合は使ってよいです。
- 呼び出し側のコードが直感的になるなら使ってよいです。
- 同じ判定や変換を複数箇所で安全に再利用できるなら使ってよいです。

「拡張関数を使ってよい場合」の判断基準として、以下の考え方を追加します。

- 対象の型だけを見れば意味が分かる処理は拡張関数に向いている
- 対象の型の自然な判定・変換・表示補助は拡張関数に向いている
- DB、API、ファイル、現在時刻、設定値、Repositoryなど外部要素に依存する処理は拡張関数に向いていない
- 拡張関数は、呼び出し側を直感的にするために使う
- ただし、責務が分かりにくくなる場合は通常の関数、UseCase、Service、Repository に置く

一方で、以下のような処理は拡張関数にせず、通常の関数、UseCase、Service、Repository に置く方針としてください。

- Repositoryを使う保存処理
- API呼び出し
- ファイルI/O
- 現在時刻に依存する処理
- 設定値に強く依存する処理
- DIコンテナや外部サービスに依存する処理

#### 拡張関数のよい例

```kotlin
fun MarketPrice.isHigherThan(other: MarketPrice): Boolean =
    this.price > other.price
```

- `MarketPrice` 同士の比較なので、対象の型に自然な処理である
- DB、API、設定値など外部要素に依存していない
- `marketPrice.isHigherThan(previousPrice)` と読めるため、呼び出し側の意図が分かりやすい
- このように、対象の型だけで完結する判定は拡張関数に向いている

```kotlin
fun TradingSignal.isBuy(): Boolean =
    this == TradingSignal.Buy
```

- `TradingSignal` が買いシグナルかどうかを表すため、ドメインの言葉として自然に読める
- `signal == TradingSignal.Buy` よりも、`signal.isBuy()` の方が「買いシグナルか？」という意図を表しやすい
- 複数箇所で同じ判定を使う場合、判定の意味を1箇所に集約できる
- ただし、1箇所でしか使わない単純比較なら、無理に拡張関数にしなくてもよい

#### 拡張関数にしない方がよい例

```kotlin
fun MarketPrice.save(repository: MarketPriceRepository) {
    repository.save(this)
}
```

- `save` は `MarketPrice` 自体の性質や判定ではなく、保存処理である
- Repositoryに依存しており、外部I/Oにつながる可能性がある
- `marketPrice.save(repository)` と書くと、保存という重要な副作用が `MarketPrice` の自然な振る舞いのように見えてしまう
- 保存処理は UseCase、Service、Repository に置いた方が責務が明確になる
- 拡張関数にすると、ドメインモデルと永続化処理の境界が曖昧になる

#### 改善例

```kotlin
class SaveMarketPriceUseCase(
    private val repository: MarketPriceRepository,
) {
    fun execute(price: MarketPrice) {
        repository.save(price)
    }
}
```

- 保存処理を UseCase に置くことで、「いつ保存するか」という処理手順が明確になる
- Repositoryへの依存が `MarketPrice` ではなく UseCase 側に集まる
- `MarketPrice` は価格データや価格に関する判定に集中できる
- 外部I/Oを伴う処理を分離できるため、テストしやすくなる

## 5. 命名ルール

- 意図が明確になる名前を選びます。処理の流れや責務が分かりにくくなる場合は、短いスコープ関数よりも明示的な変数名や関数名を優先します。

## 6. 自動売買ロジックでの特別ルール

自動売買ロジックでは、読み間違いが損失につながるため、以下の特別ルールを遵守します。

- 売買判定は名前付き関数に切り出します。
- 購入価格、現在価格、損益率、手数料、最小注文数量は明示的な変数名で扱います。
- `let` チェーンの中に売買判断を隠さないようにします。
- `also` の中で注文実行を行わないようにします。
- `run` の最後の式に重要な売買判断を隠さないようにします。
- テスト名には「何を入力したら、何を期待するか」を日本語で表します。

## 7. レビュー観点

- Scope functions や拡張関数によって、処理の流れ、責務、戻り値、`it` / `this` の意味が直感的に分かるようになっているか。
- 分かりにくくなっている場合は、通常の関数、明示的な変数名、`if`、`return` への書き換えを提案できているか。
- 自動売買ロジックにおいて、重要な判定や注文処理が明示的な名前で表現されているか。

## 8. data class の配置ルール

- `data class` は1ファイルに1つだけ定義してください。例外はありません。
- GMO API レスポンスモデルのように似た型が多い場合も、1ファイル1 `data class` にしてください。
- このルールは `ArchitectureTest` がテストコードを含む全ファイルに対して機械的に検査します。違反すると `./gradlew build` が失敗します。

## 9. KDoc必須ルール

- 検査範囲は「プロダクションコード（`src/main`）のすべての class / interface / 関数」です。追加・変更した分だけではありません。`ArchitectureTest` が機械的に検査し、KDocのない宣言が1つでもあると `./gradlew build` が失敗します。
- `public` / `internal` / `private` を問わず、すべての関数にKDocを書いてください。
- `class` / `data class` / `enum class` / `interface` にもKDocを書いてください。
- 関数のKDocには、引数がある場合は `@param` を書いてください。
- 戻り値がある場合は `@return` を書いてください。
- `class` / `data class` のKDocには、プロパティごとに `@property` を書いてください。
- KDocは実際の処理内容に合わせて書いてください。
- 別メソッドの説明をコピーしないでください。

## 10. デフォルト値のルール

- 初期資金、注文金額、最大注文額、最大保有額、APIキー名、Secret名など、運用やお金に関係する値に安易なデフォルト値を入れないでください。
- 特に `initialCapital`, `tradeAmount`, `maxOrderJpy`, `maxDailyOrderJpy`, `maxPositionJpy` などは、設定ファイルや環境変数から明示的に与える方針にしてください。
- デフォルト値を入れる場合は、安全上の理由をKDoc、テスト、PR本文に書いてください。
- 安全フラグのデフォルトは例外として許可します。
  - `real_trade_enabled=false`
  - `dry_run=true`
  - `stop_on_order_error=true`
  - `stop_on_unconfirmed_order=true`

## 11. レイヤ依存

レイヤ依存と境界のルールは [kotlin-layer-boundaries](../kotlin-layer-boundaries/SKILL.md) にまとめてあります。
`domain` に外部I/Oや `infrastructure` の型を持ち込む変更をするときは、そちらも読んでください。

## 12. 変更後に報告すること

Kotlin コードを変更したら、完了報告に以下を含めてください（該当がなければ書かなくて構いません）。

- 読みやすさのために迷った箇所と、そこでどう判断したか（Scope functions を使った/使わなかった理由、拡張関数の配置理由）。
- `./gradlew build` の実行結果。
