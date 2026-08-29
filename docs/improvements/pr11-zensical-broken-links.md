# PR11: 公開サイトのリンク切れを解消する

| 項目 | 内容 |
| --- | --- |
| 想定読者 | この改善を実施する開発者、AIコーディングエージェント |
| 読んだあとできること | 公開サイトのリンク切れを解消し、再発を検出できるようにできる |
| 状態 | 実施済み（ブランチ `docs/zensical-fix-broken-links`） |
| 機密区分 | 公開可 |
| 作成者 | リポジトリ管理者 |
| 保守責任者 | リポジトリ管理者 |
| 最終確認日 | 2026-08-30 |


## 対象の問題

[Zensical](https://zensical.org/) による [ドキュメントサイト](https://ht-0328.github.io/crypto-autotrading-lab/) を公開したことで、これまで問題にならなかったリンクの書き方が実害を出すようになっています。

Zensical がビルドするのは `docs/` 配下だけです。`docs/` の外を指す相対リンクは、GitHub 上で Markdown を直接読むときは正しく動きますが、**公開サイトでは 404 になります**。

現状の件数は次のとおりです（`docs/` 配下の全 Markdown を機械的に数えた結果）。

| 区分 | 件数 | Zensical の挙動 |
| --- | --- | --- |
| `.md` を指すもの（`AGENTS.md`、`.agents/**/SKILL.md` など）と、存在しないファイルを指すもの | 24 | ビルド時に `page does not exist` の警告が出る |
| リポジトリ直下の `README.md` を指すもの | 6 | **警告が出ないまま、サイトの外へのリンクになる** |
| `.md` 以外を指すもの（Kotlin ソース、ワークフロー、Terraform など） | 167 | **警告が出ないまま、リンク切れのページが生成される** |
| 合計 | 197 | |

つまり、ビルドの警告として現れるのは 197 件中 24 件だけです。残り 173 件は黙って壊れます。

参照先の種類ごとの内訳は次のとおりです。

| 参照先 | 件数 |
| --- | --- |
| Kotlin ソース（`projects/`） | 94 |
| GitHub ワークフロー（`.github/`） | 26 |
| Terraform（`infra/`） | 15 |
| AI エージェントのスキル（`.agents/`） | 13 |
| Docker 定義（`docker/`） | 9 |
| `AGENTS.md` | 7 |
| CI スクリプト（`ci/`） | 7 |
| 設定ファイル（`config/`） | 7 |
| リポジトリ直下の `README.md` | 6 |
| シェルスクリプト（`scripts/`） | 6 |
| テンプレートのプレースホルダ（実在しないファイル） | 3 |
| `zensical.toml` | 2 |
| WireMock スタブ（`mocks/`） | 1 |
| `.gitignore` | 1 |

特に悪いのがリポジトリ直下の `README.md` を指す 6 件です。トップページの `[リポジトリの全体像](../README.md)` は `<a href="../">` に変換され、**サイトの外（`https://ht-0328.github.io/`）へ飛びます**。読者はドキュメントから離脱します。

## なぜ直すか

- ドキュメントの価値の多くは「根拠となるコードへ辿れること」にあります。[findings.md](findings.md) や [backlog.md](backlog.md) は、指摘のたびに実装ファイルへリンクしています。このリンクが切れると、サイトの読者は指摘の裏を取れません。
- リンク切れが 198 件残っている限り、ビルドの警告を無視する運用が常態化します。そうなると、後から入った本当のリンク切れにも気付けません。
- `zensical build --strict`（警告でビルドを失敗させる）を有効にできず、リンク切れの再発を CI で止められません。

## 方針の決定

**`docs/` の外を指すリンクは、GitHub の絶対 URL に置き換えます。**

| 参照先 | 書き方 |
| --- | --- |
| `docs/` の中 | 相対パスのまま（現状維持）。例: `[setup.md](../development/setup.md)` |
| `docs/` の外のファイル | `https://github.com/ht-0328/crypto-autotrading-lab/blob/main/<パス>` |
| `docs/` の外のディレクトリ | `https://github.com/ht-0328/crypto-autotrading-lab/tree/main/<パス>` |
| 実在しないファイル（テンプレートのプレースホルダ） | リンクをやめ、`` `../specifications/features/example.md` `` のようにコード表記にする |

### 検討して採用しなかった案

| 案 | 却下理由 |
| --- | --- |
| `AGENTS.md` などを `docs/` 配下へコピーしてビルドする | 同じ内容が2箇所に存在し、片方だけ更新される。[AGENTS.md](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/AGENTS.md) は3つの AI ツールが読む正本であり、移動もコピーもできない |
| Zensical の snippets 機能で外部ファイルを取り込む | 参照先の大半は Kotlin ソース。ドキュメントに実装コードを丸ごと埋め込むことになり、実装変更のたびに陳腐化する |
| リンクをやめてファイル名のコード表記だけにする | GitHub 上で読むときの利便性が大きく落ちる。[docs/README.md](README.md) のリンク方針「ファイル名だけを文字列で書かない」にも反する |
| `docs/` の外への参照そのものをやめる | 指摘の根拠を示せなくなり、ドキュメントの価値が下がる |

絶対 URL は `main` を指すため、作業ブランチのドキュメントからは `main` 時点のコードを参照することになります。ドキュメントは「マージ後の状態」を説明するものなので、この挙動で問題ありません。

## 変更対象

このPRは**ドキュメントとワークフローのみ**を変更します。Kotlin コード、Gradle 設定、`config/` は変更しません。

| ファイル | 直す件数 |
| --- | --- |
| [findings.md](findings.md) | 47 |
| [pr10-config-fail-fast.md](pr10-config-fail-fast.md) | 18 |
| [backlog.md](backlog.md) | 12 |
| [pr02-cloud-run-config.md](pr02-cloud-run-config.md) | 12 |
| [pr05-phase1-real-order-guard.md](pr05-phase1-real-order-guard.md) | 11 |
| [pr09-ci-compose-consistency.md](pr09-ci-compose-consistency.md) | 11 |
| [plans/plan01-real-sell-order.md](../plans/plan01-real-sell-order.md) | 11 |
| [pr06-backtest-execution-model.md](pr06-backtest-execution-model.md) | 8 |
| [pr08-doc-consistency.md](pr08-doc-consistency.md) | 7 |
| [pr03-private-api-log-leak.md](pr03-private-api-log-leak.md) | 6 |
| [docs/README.md](README.md) | 4 |
| [pr04-state-repository-crash-safe.md](pr04-state-repository-crash-safe.md) | 5 |
| [plans/plan00-phase-and-safety-contract.md](../plans/plan00-phase-and-safety-contract.md) | 5 |
| [plans/plan02-order-safety-guards.md](../plans/plan02-order-safety-guards.md) | 5 |
| [plans/plan03-notification.md](../plans/plan03-notification.md) | 5 |
| [development/setup.md](../development/setup.md) | 4 |
| [pr01-docker-clean-scope.md](pr01-docker-clean-scope.md) | 4 |
| [pr07-real-order-spec-separation.md](pr07-real-order-spec-separation.md) | 4 |
| [improvements/README.md](README.md) | 3 |
| [plans/plan04-production-wiring-and-rehearsal.md](../plans/plan04-production-wiring-and-rehearsal.md) | 3 |
| [plans/README.md](../plans/README.md) | 2 |
| [specifications/phase1-simulation.md](../specifications/phase1-simulation.md) | 2 |
| [templates/design-template.md](../templates/design-template.md) | 2 |
| [infrastructure/gcp/README.md](../infrastructure/gcp/README.md) | 1 |
| [infrastructure/gcp/development-policy.md](../infrastructure/gcp/development-policy.md) | 1 |
| [operations/gcp/05-github-actions-variables.md](../operations/gcp/05-github-actions-variables.md) | 1 |
| [overview/roadmap.md](../overview/roadmap.md) | 1 |
| [plans/plan06-unattended-trading.md](../plans/plan06-unattended-trading.md) | 1 |
| [templates/specification-template.md](../templates/specification-template.md) | 1 |

加えて次の2ファイルを変更します。

| ファイル | 変更内容 |
| --- | --- |
| [docs/README.md](README.md) | 「ドキュメントリンク方針」に `docs/` の外を指すときのルールを追加 |
| [.github/workflows/docs.yml](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/.github/workflows/docs.yml) | `zensical build --clean` に `--strict` を付け、リンク切れの再発をCIで止める |
| `scripts/check-doc-links.py` | 新規作成。`docs/` の外を指す相対リンクを検出する（手順1のスクリプト） |

## 実施手順

上から順に実施してください。前の手順が終わってから次に進みます。

### 1. 置換対象を機械的に洗い出す

`docs/` 配下の Markdown から、`docs/` の外に解決される相対リンクを抽出します。手作業で探すと必ず取りこぼします。

`scripts/check-doc-links.py` として保存し、リポジトリのルートで `python3 scripts/check-doc-links.py` を実行します。

```python
import re
import pathlib
import sys

root = pathlib.Path(__file__).resolve().parent.parent
docs = root / "docs"
link_re = re.compile(r'(?<!\!)\[([^\]\[]*)\]\(([^)\s]+)(?:\s+"[^"]*")?\)')
code_re = re.compile(r"`+[^`]*`+")

found = 0
for md in sorted(docs.rglob("*.md")):
    in_fence = False
    for i, line in enumerate(md.read_text(encoding="utf-8").split("\n"), 1):
        # コードフェンスの中は対象外
        if line.lstrip().startswith("```"):
            in_fence = not in_fence
            continue
        if in_fence:
            continue
        # インラインコードの中も対象外（書き方の例としてリンクの形を載せている箇所があるため）
        line = code_re.sub(lambda m: " " * len(m.group(0)), line)
        for m in link_re.finditer(line):
            target = m.group(2)
            if target.startswith(("http", "#", "mailto:")):
                continue
            resolved = (md.parent / target.split("#")[0]).resolve()
            try:
                resolved.relative_to(docs)
                if resolved.exists():
                    continue
            except ValueError:
                pass
            print(f"{md.relative_to(root)}:{i}  {m.group(0)}")
            found += 1

print(f"{found} 件")
sys.exit(1 if found else 0)
```

**インラインコードを除外する処理を省かないでください。** [docs/README.md](README.md) のリンク方針や、この計画自身が、書き方の例としてリンクの形をした文字列を載せています。除外しないと、直す必要のない箇所まで検出されます。

### 2. 置換する

抽出した各リンクの `(...)` の中身を、`https://github.com/ht-0328/crypto-autotrading-lab/blob/main/<リポジトリルートからのパス>` に置き換えます。**リンクのラベル（`[...]` の部分）は変更しません。**

例:

```diff
-[TradingApplication](../../projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/application/TradingApplication.kt)
+[TradingApplication](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/projects/crypto-autotrading-app/src/main/kotlin/cryptoautotrading/application/TradingApplication.kt)

-[infra/terraform/gcp/](../../../infra/terraform/gcp/)
+[infra/terraform/gcp/](https://github.com/ht-0328/crypto-autotrading-lab/tree/main/infra/terraform/gcp/)
```

末尾が `/` のもの（ディレクトリ）だけ `blob` ではなく `tree` を使います。

### 3. プレースホルダのリンクを外す

実在しないファイルを指す 3 件は、絶対 URL にしても壊れたままです。リンクをやめてコード表記にします。

[templates/design-template.md](../templates/design-template.md) の 15 行目・108 行目と、[templates/specification-template.md](../templates/specification-template.md) の 111 行目を、次のように直します。

```markdown
直す前: - [対応する仕様書](../specifications/features/example.md)
直した後: - 対応する仕様書: `../specifications/features/example.md`（実際のファイル名に置き換える）
```

[docs/README.md](README.md) の「ドキュメントリンク方針」にある `phase1-overview.md` の記述は、すでにインラインコードで囲まれており実際のリンクにはなっていません。**変更不要です。**

### 4. ドキュメントリンク方針を更新する

[docs/README.md](README.md) の「ドキュメントリンク方針」に次の項目を追加します。既存の「相対パスでリンクする」は `docs/` の中に限る旨を明記します。

> - **`docs/` の外を参照するときは GitHub の絶対URLにする**
>   - 公開サイト（GitHub Pages）がビルドするのは `docs/` 配下だけです。`../../projects/...` のような相対リンクはサイト上でリンク切れになります。
>   - Kotlin ソース、ワークフロー、Terraform、`AGENTS.md`、`.agents/` などを参照する場合は `https://github.com/ht-0328/crypto-autotrading-lab/blob/main/<パス>` を使ってください。
>   - ディレクトリを指す場合は `blob` の代わりに `tree` を使います。

### 5. ビルドを --strict にする

[.github/workflows/docs.yml](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/.github/workflows/docs.yml) の `build` ジョブと `deploy` ジョブの両方で、`zensical build --clean` を `zensical build --clean --strict` に変更します。以降は `.md` へのリンク切れが1件でもあればPRのチェックが落ちます。

**この手順は 1〜4 がすべて終わってから行ってください。** 先に `--strict` にすると、このPR自身のCIが落ちます。

## 受け入れ条件

- [ ] 手順1のスクリプトの出力が 0 件であること
- [ ] `zensical build --clean --strict` が成功すること
- [ ] [.github/workflows/docs.yml](https://github.com/ht-0328/crypto-autotrading-lab/blob/main/.github/workflows/docs.yml) の両ジョブに `--strict` が入っていること
- [ ] リンクのラベルを変えていないこと（文章の意味が変わっていないこと）
- [ ] GitHub 上で Markdown を直接読んだ場合も、置き換えたリンクが正しい場所へ飛ぶこと
- [ ] Kotlin コード、Gradle 設定、`config/` に変更が無いこと

## 検証

ドキュメントとワークフローのみのため `./gradlew build` は不要です。次を実施します。

1. 手順1のスクリプトを再実行し、出力が 0 件であることを確認する。
2. `zensical build --clean --strict` が成功することを確認する。
3. `zensical serve` でサイトを開き、次のページのリンクを実際にクリックして確認する。
   - トップページの「リポジトリの全体像」がリポジトリの `README.md` へ飛ぶこと
   - [findings.md](findings.md) の実装ファイルへのリンクが GitHub のソース表示へ飛ぶこと
   - [docs/README.md](README.md) の `AGENTS.md` へのリンクが飛ぶこと

## スコープ外

- 公開サイトでの読みやすさの改善（[pr12-zensical-readability.md](pr12-zensical-readability.md)）
- ドキュメントの内容そのものの修正。**リンク先を変えるだけで、文章は書き換えません。**
- `.md` 以外へのリンク切れを CI で検出する仕組み。`--strict` でも検出できませんが、手順1のスクリプトで代替できるため今回は入れません。必要になったら [backlog.md](backlog.md) に登録してください。
