FROM openjdk:8-alpine

WORKDIR /app

COPY ./target/yuoj-code-sandbox-0.0.1-SNAPSHOT.jar /app/my-sanbox.jar

CMD ["java", "-jar", "my-sanbox.jar"]
