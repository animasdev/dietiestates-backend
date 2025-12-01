# syntax=docker/dockerfile:1

# Multi-stage build for Spring Boot app (Java 21)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy Maven descriptor first to leverage layer caching
COPY pom.xml ./

# Copy sources and docs (OpenAPI used during generate-sources)
COPY src ./src
COPY docs ./docs

# Build application (skip tests inside container)
RUN mvn -B -DskipTests package

# Runtime image (JRE only)
FROM eclipse-temurin:21-jre
WORKDIR /app

ENV JAVA_OPTS=""
EXPOSE 8080

# Copy the fat jar produced by Spring Boot plugin
COPY --from=build /app/target/*.jar /app/app.jar

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]

