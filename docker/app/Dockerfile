# ベースイメージ: public.ecr.aws/docker/library/eclipse-temurin:17.0.13_11-jdk-jammy
FROM public.ecr.aws/docker/library/eclipse-temurin:17.0.13_11-jdk-jammy

ENV TZ=Asia/Tokyo
ENV LANG=C.UTF-8

RUN set -eux && \
    apt-get update && \
    apt-get install -y tzdata && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy application artifacts (e.g. from gradle build)
COPY build/libs/crypto-autotrading-lab-1.0-SNAPSHOT-all.jar app.jar

CMD ["java", "-jar", "app.jar"]
