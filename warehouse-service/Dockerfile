FROM maven:3-eclipse-temurin-21 AS builder
COPY pom.xml pom.xml
COPY src src
RUN mvn clean install -Dmaven.test.skip=true

FROM openjdk:21-slim
ENV TZ=Europe/Warsaw
RUN apt-get update && apt-get install -y tzdata && \
    ln -snf /usr/share/zoneinfo/Europe/Warsaw /etc/localtime && \
    echo "Europe/Warsaw" > /etc/timezone && \
    apt-get clean
COPY --from=builder /target/warehouse-service-*.jar /app.jar
EXPOSE 8080
CMD ["java", "-jar", "/app.jar"]
