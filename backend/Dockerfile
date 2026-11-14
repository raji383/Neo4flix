# syntax=docker/dockerfile:1.7

FROM maven:3.9.16-eclipse-temurin-21 AS build

ARG SERVICE
WORKDIR /workspace

COPY ${SERVICE}/pom.xml ./pom.xml
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn --batch-mode dependency:go-offline

COPY ${SERVICE}/src ./src
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn --batch-mode -DskipTests package

FROM eclipse-temurin:21.0.13_9-jre-alpine

RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app
COPY --from=build --chown=spring:spring /workspace/target/*.jar ./app.jar

USER spring:spring

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
