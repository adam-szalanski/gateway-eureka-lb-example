FROM openjdk:21-slim

ENV TZ=Europe/Warsaw

RUN apt-get update && apt-get install -y tzdata && \
    ln -snf /usr/share/zoneinfo/Europe/Warsaw /etc/localtime && \
    echo "Europe/Warsaw" > /etc/timezone && \
    apt-get clean

COPY target/gateway-*.jar /app.jar

CMD ["java", "-jar", "/app.jar"]
