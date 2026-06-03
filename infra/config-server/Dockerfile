FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY . .

RUN mvn clean package -DskipTests -pl infra/config-server -am

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/infra/config-server/target/config-server-*.jar app.jar

EXPOSE 8888

ENTRYPOINT ["java", "-jar", "app.jar"]
