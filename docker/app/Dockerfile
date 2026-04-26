FROM eclipse-temurin:17-jre-jammy

ENV TZ=Asia/Tokyo
ENV LANG=C.UTF-8

WORKDIR /app

COPY build/libs/*-all.jar app.jar

CMD ["java", "-jar", "app.jar"]
