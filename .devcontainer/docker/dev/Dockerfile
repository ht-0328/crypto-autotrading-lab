# ベースイメージ: public.ecr.aws/docker/library/eclipse-temurin:17.0.13_11-jdk-jammy
FROM public.ecr.aws/docker/library/eclipse-temurin:17.0.13_11-jdk-jammy

# パッケージのインストール時に対話的なプロンプトが表示されないように設定する
ENV DEBIAN_FRONTEND=noninteractive

# タイムゾーン・ロケール設定
ENV TZ=Asia/Tokyo
ENV LANG=C.UTF-8

# 必要なパッケージのインストール
RUN set -eux && \
    apt-get update && \
    apt-get install -y \
    curl \
    unzip \
    zip \
    git \
    bash-completion \
    tzdata && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

###### Kotlinコンパイラのダウンロードとインストール ######

# Kotlinコンパイラのバージョン
ARG KOTLIN_VERSION=2.1.10
ARG BASE_URL_KOTLIN=https://github.com/JetBrains/kotlin/releases/download/

# 環境変数の設定
ENV KOTLIN_HOME=/opt/kotlin/kotlin-compiler-${KOTLIN_VERSION}
ENV PATH="${KOTLIN_HOME}/bin:${PATH}"

# Kotlinのダウンロードと展開
RUN set -eux && \
    curl -L -o /tmp/kotlin-compiler.zip ${BASE_URL_KOTLIN}/v${KOTLIN_VERSION}/kotlin-compiler-${KOTLIN_VERSION}.zip && \
    unzip /tmp/kotlin-compiler.zip -d /opt/kotlin && \
    rm /tmp/kotlin-compiler.zip

###### Gradleのダウンロードとインストール ######

# Gradleのバージョン
ARG GRADLE_VERSION=8.13
ARG BASE_URL_GRADLE=https://services.gradle.org/distributions

# 環境変数の設定
ENV GRADLE_HOME=/opt/gradle/gradle-${GRADLE_VERSION}
ENV PATH="${GRADLE_HOME}/bin:${PATH}"

# Gradleのダウンロードと展開
RUN set -eux && \
    curl -L -o /tmp/gradle.zip ${BASE_URL_GRADLE}/gradle-${GRADLE_VERSION}-bin.zip && \
    unzip /tmp/gradle.zip -d /opt/gradle && \
    rm /tmp/gradle.zip

###### Google Cloud CLIのインストール ######

RUN set -eux && \
    apt-get update && \
    apt-get install -y apt-transport-https ca-certificates gnupg && \
    curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | gpg --dearmor -o /usr/share/keyrings/cloud.google.gpg && \
    echo "deb [signed-by=/usr/share/keyrings/cloud.google.gpg] https://packages.cloud.google.com/apt cloud-sdk main" | tee -a /etc/apt/sources.list.d/google-cloud-sdk.list && \
    apt-get update && \
    apt-get install -y google-cloud-cli && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 作業ディレクトリの設定
WORKDIR /workspace

# VSCode DevContainer 用の環境変数
ENV DEVCONTAINER=true
