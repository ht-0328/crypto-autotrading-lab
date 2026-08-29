FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /app

COPY . .

RUN cd projects/crypto-autotrading-app && ./gradlew clean shadowJar --no-daemon

FROM eclipse-temurin:17-jre-jammy

ENV TZ=Asia/Tokyo
ENV LANG=C.UTF-8
ENV APP_DATA_DIR=/app/data
# ConfigLoader は「設定ファイルを土台に環境変数で上書き」する設計のため、土台となる設定を同梱する
ENV APP_CONFIG_PATH=/app/config/application-gmo.yaml

WORKDIR /app

COPY --from=builder /app/projects/crypto-autotrading-app/build/libs/*-all.jar app.jar
COPY config/ /app/config/

CMD ["java", "-jar", "app.jar"]
