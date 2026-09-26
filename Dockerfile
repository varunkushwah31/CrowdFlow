# syntax=docker/dockerfile:1
# Multi-stage Dockerfile for CrowdFlow (Java 25 LTS & Spring Boot 3.4)

# ================================
# Stage 1: Build & Package
# ================================
FROM maven:3.9.9-eclipse-temurin-21 AS build
# Note: Maven builder compiles bytecode for target release 25
WORKDIR /workspace

# Cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source code and package application
COPY src src
RUN mvn clean package -DskipTests -B

# ================================
# Stage 2: Minimal Production Runtime
# ================================
FROM eclipse-temurin:25-jre-noble AS runtime

LABEL maintainer="CrowdFlow Civic Tech Team <admin@crowdflow.civic.in>"
LABEL description="Production Container for CrowdFlow Civic Water Monitoring Monolith (India Edition)"

WORKDIR /app

# Non-root civic user for Linux security compliance
RUN groupadd -r civic && useradd -r -g civic civic && \
    mkdir -p /app/uploads /app/generated-reports /app/data && \
    chown -R civic:civic /app

USER civic

# Copy compiled executable JAR from builder stage
COPY --from=build --chown=civic:civic /workspace/target/*.jar /app/app.jar

# Expose HTTP port
EXPOSE 8085

# Java 25 Virtual Threads & JVM optimizations
ENV JAVA_OPTS="-Dspring.classformat.ignore=true -XX:+UseZGC -XX:+ZGenerational -Xms512m -Xmx1024m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
