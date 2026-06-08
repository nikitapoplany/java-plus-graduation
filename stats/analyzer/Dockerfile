FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY . .

RUN mvn clean package -DskipTests -pl stats/analyzer -am

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/stats/analyzer/target/analyzer-*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
