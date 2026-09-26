# syntax=docker/dockerfile:1
# ==============================================================================
# CrowdFlow India - Enterprise Dockerfile (Java 25 LTS & Spring Boot 3.4)
# Multi-stage build with Layered dependency extraction for fast caching
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Dependency & Application Extractor
# ------------------------------------------------------------------------------
FROM eclipse-temurin:25-jdk-noble AS extractor
WORKDIR /build

ARG JAR_FILE=target/waterwatch-platform-1.0.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

# Spring Boot 3.4 extract creates lib/ (dependencies) and thin app.jar (application)
RUN java -Djarmode=tools -jar app.jar extract --destination extracted

# ------------------------------------------------------------------------------
# Stage 2: Hardened, Minimal Production Runtime
# ------------------------------------------------------------------------------
FROM eclipse-temurin:25-jre-noble AS runtime

LABEL org.opencontainers.image.title="CrowdFlow India Civic Water Monitoring Platform" \
      org.opencontainers.image.description="Crowdsourced civic water infrastructure monitoring and resolution platform for India" \
      org.opencontainers.image.version="1.0.0" \
      org.opencontainers.image.vendor="CrowdFlow Civic Tech Team" \
      org.opencontainers.image.licenses="Apache-2.0"

# Install curl for container health check & tzdata for Indian Standard Time
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl tzdata && \
    rm -rf /var/lib/apt/lists/*

# Set Indian Standard Timezone (Asia/Kolkata) as default
ENV TZ=Asia/Kolkata
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR /app

# Create unprivileged system user & group (UID/GID 10001) for container security
RUN groupadd -g 10001 civic && \
    useradd -u 10001 -g civic -m -s /bin/bash civic && \
    mkdir -p /app/uploads /app/generated-reports /app/data && \
    chown -R civic:civic /app

# 1. Copy dependencies (lib/) first - this layer changes rarely and is cached
COPY --from=extractor --chown=civic:civic /build/extracted/lib/ /app/lib/

# 2. Copy thin application JAR second - only this small layer changes on code update
COPY --from=extractor --chown=civic:civic /build/extracted/*.jar /app/app.jar

USER civic:civic

# Expose HTTP port
EXPOSE 8085

# Java 25 JVM configuration:
# - ZGC with low-latency pause times (< 1ms)
# - Headless mode for SVG & PDF generation
# - Explicit UTF-8 file encoding
ENV JAVA_OPTS="-Dspring.classformat.ignore=true -XX:+UseZGC -Xms256m -Xmx1024m -Djava.awt.headless=true -Dfile.encoding=UTF-8"

# Built-in Docker Healthcheck against Actuator endpoint
HEALTHCHECK --interval=15s --timeout=5s --start-period=25s --retries=3 \
    CMD curl -f http://localhost:8085/actuator/health || exit 1

# Use exec format with Java for proper SIGTERM signal handling during container stop
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
