# 開発環境

## 開発前提

* 開発は VS Code devcontainer 前提
* Kotlin 開発支援には **必ず `Kotlin/kotlin-lsp` を使う**
  * https://github.com/Kotlin/kotlin-lsp
  * ※ 本拡張機能は VS Code Marketplace に登録されていない場合があるため、その際は公式の Releases ページから VSIX をダウンロードし、手動でインストールしてください。
* アプリ本体は `projects/crypto-autotrading-app/` にある
* 参考リポジトリは開発環境構成の参考であり、アプリ本体は Kotlin CLI とする
* Spring Boot 構成にはしない

## 開発環境の参考

以下のリポジトリの **開発環境構成** を参考にすること。

* https://github.com/ht-0328/Kotlin-SpringBoot-OpenAPI/tree/main

## 開発時の確認コマンド

```bash
cd projects/crypto-autotrading-app
./gradlew build
./gradlew run
```

```bash
docker compose -f docker/compose/local.yml up --build
```

## 実行環境方針

* 最初はローカルの Docker コンテナで実行
* 将来は AWS やレンタルサーバー等に載せやすい構成を目指す
* ただし今回はクラウド対応は不要
