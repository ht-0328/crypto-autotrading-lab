FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /app

COPY . .

RUN cd projects/crypto-autotrading-app && ./gradlew clean shadowJar --no-daemon

FROM eclipse-temurin:17-jre-jammy

ENV TZ=Asia/Tokyo
ENV LANG=C.UTF-8
ENV APP_DATA_DIR=/app/data

WORKDIR /app

COPY --from=builder /app/projects/crypto-autotrading-app/build/libs/*-all.jar app.jar

CMD ["java", "-jar", "app.jar"]
