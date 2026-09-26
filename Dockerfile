# syntax=docker/dockerfile:1
# Container for CrowdFlow (Java 25 LTS & Spring Boot 3.4)

FROM eclipse-temurin:25-jre-noble AS runtime

LABEL maintainer="CrowdFlow Civic Tech Team <admin@crowdflow.civic.in>"
LABEL description="Container for CrowdFlow Civic Water Monitoring Monolith (India Edition)"

WORKDIR /app

# Non-root civic user for Linux security compliance
RUN groupadd -r civic && useradd -r -g civic civic && \
    mkdir -p /app/uploads /app/generated-reports /app/data && \
    chown -R civic:civic /app

USER civic

# Copy compiled executable JAR from target directory
COPY --chown=civic:civic target/waterwatch-platform-1.0.0-SNAPSHOT.jar /app/app.jar

# Expose HTTP port
EXPOSE 8085

# Java 25 Virtual Threads & JVM optimizations
ENV JAVA_OPTS="-Dspring.classformat.ignore=true -XX:+UseZGC -Xms512m -Xmx1024m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
