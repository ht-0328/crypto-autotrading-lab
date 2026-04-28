FROM eclipse-temurin:17-jre-jammy

ENV TZ=Asia/Tokyo
ENV LANG=C.UTF-8
ENV APP_DATA_DIR=/app/data

WORKDIR /app

COPY projects/crypto-autotrading-app/build/libs/*-all.jar app.jar

CMD ["java", "-jar", "app.jar"]
