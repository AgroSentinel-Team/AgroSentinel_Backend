FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "if [ \"${AGROSENTINEL_DEMO_DATA_ENABLED:-false}\" = \"true\" ]; then exec java -jar app.jar --spring.profiles.active=demo-data \"$@\"; else exec java -jar app.jar \"$@\"; fi", "--"]