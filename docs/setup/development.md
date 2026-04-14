# 開発環境

## 開発前提

* VS Code を使う
* devcontainer で開発する
* Kotlin 開発支援には **必ず `Kotlin/kotlin-lsp` を使う**
  * https://github.com/Kotlin/kotlin-lsp
  * ※ 本拡張機能は VS Code Marketplace に登録されていない場合があるため、その際は公式の Releases ページから VSIX をダウンロードし、手動でインストールしてください。

## 開発環境の参考

以下のリポジトリの **開発環境構成** を参考にすること。

* https://github.com/ht-0328/Kotlin-SpringBoot-OpenAPI/tree/main

## 開発時の確認コマンド

* `./gradlew build`
* `./gradlew run`
* `docker compose up --build`

## 実行環境方針

* 最初はローカルの Docker コンテナで実行
* 将来は AWS やレンタルサーバー等に載せやすい構成を目指す
* ただし今回はクラウド対応は不要
