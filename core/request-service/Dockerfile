FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY . .

RUN mvn clean package -DskipTests -pl core/request-service -am

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/core/request-service/target/request-service-*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
