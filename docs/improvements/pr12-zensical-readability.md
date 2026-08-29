# PR12: 公開サイトでの読みやすさを Zensical に合わせる

**状態**: 実施済み（ブランチ `docs/zensical-readability`）

## 対象の問題

[ドキュメントサイト](https://ht-0328.github.io/crypto-autotrading-lab/) を公開したことで、既存ドキュメントの書き方のうち「GitHub 上で読む前提」で書かれていた部分が目立つようになりました。リンク切れ（[pr11-zensical-broken-links.md](pr11-zensical-broken-links.md)）とは別に、次の4点があります。

### A. 安全に関わる警告が本文に埋もれる

このリポジトリで最も重要な情報は「**やってはいけないこと**」です。しかし現状は引用記法（`> **注意**: ...`）や太字で書かれており、サイト上では通常の段落とほとんど見分けが付きません。

[zensical.toml](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/zensical.toml) では `admonition` と `pymdownx.details` を有効にしてあり、危険度に応じた色付きのボックスを使えます。使っていないだけです。

該当する箇所は 17 件あります（引用記法のコールアウト）。特に重要なのは次の3つです。

- [plans/README.md](../plans/README.md) の「最重要: いま実注文を有効にすると何が起きるか」— 売り注文が送られないまま含み損を抱える説明。このリポジトリで一番読ませたい内容です。
- [specifications/features/real-trading-gmo-order.md](../specifications/features/real-trading-gmo-order.md) の冒頭「この仕様は Phase3 のスコープです」— 読み違えると Phase1 で実注文を試みることになります。
- [operations/real-trading-recovery.md](../operations/real-trading-recovery.md) の「Phase1 では実注文を行いません」

### B. 章の見出しと、その中のページの見出しが同じ

[infrastructure/gcp/README.md](../infrastructure/gcp/README.md) と [infrastructure/gcp/development-policy.md](../infrastructure/gcp/development-policy.md) は、どちらも H1 が「GCP インフラコード設計書」です。

サイトの目次では「インフラ設計 > GCP インフラコード設計書 > GCP インフラコード設計書」と同じ名前が2段続きます。どちらを開けばよいのか分かりません。README は「置き場の案内」、`development-policy.md` は「設計書の本体」であり、役割が違います。

### C. トップページのディレクトリ一覧に `infrastructure/` が無い

[docs/README.md](README.md) の「ディレクトリ構成」には 8 つのディレクトリが並んでいますが、`infrastructure/` だけ抜けています。サイトのサイドバーには「インフラ設計」の章が出るため、説明と実物が食い違います。

### D. ドキュメントを追加しても目次に出ない

サイトの章立ては [zensical.toml](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/zensical.toml) の `nav` が決めています。`nav` に書かれていないファイルはビルドはされますが、**サイドバーからは辿れません**。

このルールがドキュメント側に書かれていないため、次に文書を追加した人（AIエージェント含む）は確実に忘れます。

## なぜ直すか

- **A** は安全に直結します。このリポジトリは「判断に幅があるときは常に安全側に倒す」方針（[AGENTS.md](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/AGENTS.md)）で運用しており、警告が読み飛ばされる状態を放置すべきではありません。
- **B** と **C** は、読み手がどのページを開けばよいか判断できない状態です。ドキュメントを整理した意味が薄れます。
- **D** は放置すると壊れ続けます。追加した文書が目次に出ないことに気付くのは、たいてい必要になったときです。

## 変更対象

このPRは**ドキュメントとサイト設定のみ**を変更します。Kotlin コード、Gradle 設定、`config/` は変更しません。

| ファイル | 変更内容 | 対象 |
| --- | --- | --- |
| [plans/README.md](../plans/README.md) | 「最重要」の節を `!!! danger` に | A |
| [docs/README.md](README.md) | 「リアル注文について」を `!!! warning` に。ディレクトリ構成に `infrastructure/` を追加。リンク方針に `nav` の更新ルールを追加 | A / C / D |
| [specifications/features/real-trading-gmo-order.md](../specifications/features/real-trading-gmo-order.md) | 冒頭の Phase3 注記を `!!! warning` に。他4件を `!!! note` に | A |
| [operations/real-trading-recovery.md](../operations/real-trading-recovery.md) | Phase1 の注記を `!!! warning` に | A |
| [operations/gcp/06-deploy-cloud-run-job.md](../operations/gcp/06-deploy-cloud-run-job.md) | Phase1 の注意を `!!! warning` に | A |
| [overview/product.md](../overview/product.md) | 「ロックの限界」を `!!! warning` に | A |
| [plans/plan00-phase-and-safety-contract.md](../plans/plan00-phase-and-safety-contract.md) | Scheduler 停止の注意を `!!! warning` に | A |
| [plans/plan05-canary-with-real-money.md](../plans/plan05-canary-with-real-money.md) | 手順書への案内を `!!! tip` に | A |
| [operations/gcp/01-account-and-project.md](../operations/gcp/01-account-and-project.md) | 注意を `!!! note` に | A |
| [operations/gcp/04-service-accounts-and-iam.md](../operations/gcp/04-service-accounts-and-iam.md) | Note を `!!! note` に | A |
| [operations/gcp/05-github-actions-variables.md](../operations/gcp/05-github-actions-variables.md) | 4件の補足を `!!! note` に | A |
| [operations/gcp/08-cleanup.md](../operations/gcp/08-cleanup.md) | 「（※1）自動では消さないもの」を `!!! note` に | A |
| [infrastructure/gcp/README.md](../infrastructure/gcp/README.md) | H1 を「インフラ設計 (Infrastructure)」に変更し、置き場の案内であることを明確にする | B |
| [zensical.toml](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/zensical.toml) | 変更不要（章名は `nav` 側で「インフラ設計」と定義済みのため、README の H1 を直すだけで重複が解消する） | B |
| [development/workflow.md](../development/workflow.md) | ドキュメント追加時に `nav` を更新する手順を追加 | D |

## 実施手順

### 1. 引用記法のコールアウトを admonition にする

対象は次のコマンドで抽出できます。

```bash
grep -rn '^> \*\*' docs/
```

**[改善計画](README.md) 配下のPRファイルにある引用は変換しません。** これらは「別の文書にこう書き込む」という文面を引用しているものであり、そのページ自身の警告ではありません。admonition にすると、引用なのか、そのページの警告なのかが読み取れなくなります。

危険度の対応は次のとおりです。**勝手に増減させず、この表に従ってください。**

| 内容 | 記法 | 色 |
| --- | --- | --- |
| 実資金の損失につながる、または取り返しがつかない | `!!! danger "見出し"` | 赤 |
| 読み違えると危険な操作をしてしまう（Phase の取り違えなど） | `!!! warning "見出し"` | 橙 |
| 補足・前提の共有 | `!!! note "見出し"` | 青 |
| 別のページへの案内 | `!!! tip "見出し"` | 緑 |

変換の例:

```diff
-> **リアル注文について**: [リアル購入処理の仕様](specifications/features/real-trading-gmo-order.md) は **Phase3** のスコープです。コードは先行実装されていますが、Phase1 では起動時ガードにより実行できません。
+!!! warning "リアル注文について"
+
+    [リアル購入処理の仕様](specifications/features/real-trading-gmo-order.md) は **Phase3** のスコープです。コードは先行実装されていますが、Phase1 では起動時ガードにより実行できません。
```

**本文は4スペースでインデントします。** インデントを忘れると admonition の外に出ます。

[plans/README.md](../plans/README.md) の「最重要」は節（H2）ごと `!!! danger "いま実注文を有効にすると何が起きるか"` に入れます。中の箇条書きと段落もすべて4スペースでインデントしてください。

### 2. インフラ設計の章タイトルを直す

[infrastructure/gcp/README.md](../infrastructure/gcp/README.md) の H1 を「GCP インフラコード設計書」から「インフラ設計 (Infrastructure)」に変更します。本文は、ここが置き場の案内であり、設計の本体は [development-policy.md](../infrastructure/gcp/development-policy.md) であることが分かる書き方にします。

他のディレクトリの README（[architecture/README.md](../architecture/README.md)、[operations/README.md](../operations/README.md) など）が「設計 (Architecture)」「運用 (Operations)」という形式で統一されているので、それに合わせます。

あわせて [zensical.toml](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/zensical.toml) の `nav` を確認し、「インフラ設計」章の見え方が二重にならないことを確認します。

### 3. ディレクトリ構成に infrastructure/ を追加する

[docs/README.md](README.md) の「ディレクトリ構成」に次を追加します。並び順は既存の流れ（`operations/` の後）に合わせます。

> - `infrastructure/`: GCP インフラをコードで構築するための設計。運用手順は `operations/` 側にあります。

### 4. nav の更新ルールをドキュメント化する

[development/workflow.md](../development/workflow.md) に、ドキュメントを追加・移動・削除したときの手順として次を追加します。

> `docs/` 配下に Markdown を追加・移動・削除したら、[zensical.toml](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/zensical.toml) の `nav` も必ず更新してください。`nav` に無いファイルは公開サイトの目次に出ません。

[docs/README.md](README.md) の「ドキュメントリンク方針」にも同じ趣旨を1行入れ、リンク方針とセットで目に入るようにします。

## 受け入れ条件

- [ ] `grep -rn '^> ' docs/` に残る引用が、他の文書へ書き込む文面を引用している箇所だけであること（[改善計画](README.md) 配下の各PRファイル）
- [ ] `zensical build --clean` が成功し、警告が [pr11-zensical-broken-links.md](pr11-zensical-broken-links.md) 実施前より増えていないこと
- [ ] admonition の中身が4スペースでインデントされ、サイト上で色付きボックスとして表示されること
- [ ] [plans/README.md](../plans/README.md) の「いま実注文を有効にすると何が起きるか」が赤いボックスで表示されること
- [ ] サイトの目次で「インフラ設計」の下に同名のページが並んでいないこと
- [ ] [docs/README.md](README.md) のディレクトリ構成に `infrastructure/` があること
- [ ] `nav` の更新ルールが [development/workflow.md](../development/workflow.md) と [docs/README.md](README.md) の両方から辿れること
- [ ] 警告の**内容**を書き換えていないこと（記法だけを変える）
- [ ] Kotlin コード、Gradle 設定、`config/` に変更が無いこと

## 検証

ドキュメントとサイト設定のみのため `./gradlew build` は不要です。次を実施します。

1. `zensical build --clean` が成功することを確認する。
2. `zensical serve` でサイトを開き、admonition が色付きで表示されることを目視で確認する。特に [plans/README.md](../plans/README.md) が赤いボックスになっていること。
3. サイドバーで「インフラ設計」を開き、同じ名前が2段続いていないことを確認する。
4. GitHub 上で同じ Markdown を表示し、admonition 部分が極端に読みにくくなっていないことを確認する。

## スコープ外

- **リンク切れの解消**（[pr11-zensical-broken-links.md](pr11-zensical-broken-links.md)）。先にそちらを実施してください。
- **文書の鮮度管理（「状態」「最終確認日」）を全文書へ展開すること**。現在は [templates/](../templates/) にしか無く、実際の仕様書・設計書には付いていません。範囲が広く、内容の確認作業が伴うため、このPRには含めません。必要なら [backlog.md](backlog.md) に登録してください。
- **サイトのテーマの作り込み**（ロゴ、独自CSS、フォント）。まず内容を整えることを優先します。
- **検索UIの日本語化**。Zensical の検索インターフェースは現時点で英語のみです（検索そのものは日本語で動きます）。Zensical 側の対応待ちで、こちらでは変更できません。
- **GitHub 上での見た目**。admonition は GitHub の Markdown では見出しと本文が単純なテキストとして表示されます。読めなくはなりませんが、色は付きません。公開サイトを正とします。
