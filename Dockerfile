# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY docker/settings.xml /usr/share/maven/conf/settings.xml
COPY pom.xml .

# Download dependencies first (cached if pom.xml doesn't change)
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="TinyHIS Team"
LABEL description="TinyHIS - Lightweight Hospital Information System"

# Set working directory
WORKDIR /app

# Create non-root user for security
RUN addgroup -S tinyhis && adduser -S tinyhis -G tinyhis

# Copy the built jar file from build stage
COPY --from=build /app/target/tinyhis-*.jar app.jar

# Create directories for knowledge base, vector index, and logs
RUN mkdir -p /app/medical-knowledge /app/vector-index /app/logs && \
    chown -R tinyhis:tinyhis /app

# Switch to non-root user
USER tinyhis

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM options for container environment
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseContainerSupport"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
