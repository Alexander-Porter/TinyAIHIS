# TinyHIS Backend Dockerfile
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="TinyHIS Team"
LABEL description="TinyHIS - Lightweight Hospital Information System"

# Set working directory
WORKDIR /app

# Create non-root user for security
RUN addgroup -S tinyhis && adduser -S tinyhis -G tinyhis

# Copy the built jar file
COPY target/tinyhis-*.jar app.jar

# Create directories for knowledge base and logs
RUN mkdir -p /app/medical-knowledge /app/logs && \
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
