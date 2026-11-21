FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./pom.xml
RUN mvn -f pom.xml dependency:go-offline -DskipTests
COPY src ./src
RUN mvn -f pom.xml package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/target/*.jar app.jar
ENV REGISTRY_PORT=8761
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
