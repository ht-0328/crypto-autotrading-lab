# GCP アカウントとプロジェクトのセットアップ

## 概要
GCPアカウントとプロジェクトのセットアップ

## 対象読者
運用インフラ構築担当者

## この文書で分かること
GCPの初期準備、プロジェクト作成、課金設定

## 関連ドキュメント
[github-actions-gcp-deploy-setup.md](github-actions-gcp-deploy-setup.md)

## 前提
本ドキュメントの記載内容は、Phase1（シミュレーション環境）を前提としています。

## 本文
## このドキュメントの目的
このドキュメントは、GCP (Google Cloud Platform) を初めて使う人向けの初期準備手順を説明します。
GitHub Actions から GCP へデプロイするための設定に入る前に、まずは GCP アカウントの準備、GCP プロジェクトの作成、課金設定、および手元の PC で `gcloud` CLI を使えるようにする必要があります。
このドキュメントの準備が終わったら、次に `docs/human/github-actions-gcp-deploy-setup.md` を読んでデプロイの準備に進んでください。

## 全体の流れ
以下の順番で準備を進めます。

1. Google アカウントを用意する
2. Google Cloud にアクセスする
3. GCP プロジェクトを作成する
4. 課金アカウントを設定する
5. gcloud CLI をインストールする
6. gcloud CLI でログインする
7. 作成した GCP プロジェクトを選択する
8. プロジェクト ID とプロジェクト番号を確認する

---

## 1. Google アカウントの準備
Google Cloud を使うには、ベースとなる Google アカウントが必要です。
- 個人開発の場合、普段お使いの個人の Google アカウントで問題ありません。
- 会社やチームで使う場合は、個人アカウントではなく組織で管理されているアカウント（Google Workspace アカウントなど）を使用することを強く推奨します。

## 2. Google Cloud にアクセスする
お使いの Google アカウントで、[Google Cloud Console](https://console.cloud.google.com/) にアクセスします。
初回アクセスの場合は、利用規約の同意画面が表示されるので確認して同意してください。

## 3. GCP プロジェクトを作成する
Google Cloud 上の全てのリソースは「プロジェクト」という単位で管理されます。

1. 画面上部にある「プロジェクトの選択」ドロップダウンを開きます。
2. 「新しいプロジェクト」をクリックします。
3. **プロジェクト名** を入力します。
4. プロジェクト名の下に **プロジェクト ID** が表示されます（自動生成されますが、「編集」から好きな ID に変更することも可能です）。
   - *注意: プロジェクト ID は後から簡単には変更できないため、慎重に決めてください。*
5. 「作成」ボタンを押します。

> **Note:** 以降の手順では、作成したご自身のプロジェクト ID を `<YOUR_GCP_PROJECT_ID>` と読み替えてください。

## 4. 課金設定
Cloud Build、Artifact Registry、Cloud Run、Cloud Storage などのサービスを利用するには、プロジェクトに課金アカウントを紐づける必要があります。

- 無料枠が用意されているサービスも多いですが、使い方によっては料金が発生する可能性があるため、完全無料の保証はありません。
- 個人開発では、不要になったリソース（Cloud Run のジョブ、保存された古い Docker イメージなど）を適宜削除することが無駄な出費を防ぐために重要です。
- Cloud Console の左側メニューから「お支払い（Billing）」を選択し、クレジットカード情報などを登録して課金アカウントを作成し、現在のプロジェクトに紐づけてください。

## 5. gcloud CLI の準備
`gcloud` CLI は、ターミナルから Google Cloud の各種サービスを操作するための公式コマンドラインツールです。

- インストール方法は OS（Windows, macOS, Linux）によって異なります。
- [公式のインストール手順](https://cloud.google.com/sdk/docs/install) に従ってインストールしてください。
- インストールが完了したら、ターミナルで `gcloud --version` と入力し、バージョンが表示されることを確認します。

## 6. gcloud CLI のログイン
ターミナルを開き、以下のコマンドを実行して Google Cloud にログインします。

```bash
gcloud auth login
```

ブラウザが開き、Google アカウントの選択画面が表示されます。プロジェクトを作成したアカウントを選択してアクセスを許可してください。

## 7. 使用する GCP プロジェクトの設定
これから実行する `gcloud` コマンドが対象とするプロジェクトを設定します。
まず、環境変数にあなたのプロジェクト ID を設定します。以下のコマンドの `<YOUR_GCP_PROJECT_ID>` の部分を、実際に作成したプロジェクト ID に書き換えて実行してください。

```bash
export PROJECT_ID="<YOUR_GCP_PROJECT_ID>"

gcloud config set project "$PROJECT_ID"
```

設定が正しく反映されたか確認します。

```bash
gcloud config get-value project
```

**期待する結果**: 設定した `<YOUR_GCP_PROJECT_ID>` が表示されること。

## 8. プロジェクトの状態確認
プロジェクトが正常にアクティブになっているか確認します。

```bash
gcloud projects describe "$PROJECT_ID" \
  --format="table(projectId,projectNumber,lifecycleState)"
```

**期待する結果**:
- `lifecycleState` が `ACTIVE` になっていること。

## 9. 課金の確認
プロジェクトの課金設定が有効になっているか確認します。

```bash
gcloud billing projects describe "$PROJECT_ID" \
  --format="table(billingAccountName,billingEnabled)"
```

**期待する結果**:
- `billingEnabled` が `True` になっていること。

---

## このドキュメントの完了条件
以下のチェックリストがすべて「はい」になったら、このドキュメントの作業は完了です。

- [ ] Google アカウントで Google Cloud にアクセスできる
- [ ] GCP プロジェクトを作成済み
- [ ] GCP プロジェクトに課金アカウントが紐づいている
- [ ] `gcloud` CLI でログイン済み (`gcloud auth login`)
- [ ] `gcloud config get-value project` で対象プロジェクトが表示される
- [ ] `gcloud projects describe` の結果、`lifecycleState` が `ACTIVE` であることが確認できる
- [ ] `gcloud billing projects describe` の結果、`billingEnabled` が `True` になっていることが確認できる

以上の準備が整ったら、次は `docs/human/github-actions-gcp-deploy-setup.md` に進んでください。


## 注意点
特にありません。

## 更新タイミング
システムの要件や運用フローが変更された際に更新してください。