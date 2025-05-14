FROM maven:3-eclipse-temurin-21 AS builder
COPY pom.xml pom.xml
COPY src src
RUN mvn clean install -Dmaven.test.skip=true

FROM openjdk:21-slim
ENV TZ=Europe/Warsaw
RUN apt-get update && \
    apt-get install -y --no-install-recommends tzdata curl && \
    ln -snf /usr/share/zoneinfo/${TZ} /etc/localtime && \
    echo "${TZ}" > /etc/timezone && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*
COPY --from=builder /target/eureka-*.jar /app.jar
EXPOSE 8761
CMD ["java", "-jar", "/app.jar"]
