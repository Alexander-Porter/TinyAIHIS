#!/bin/sh
set -e

# Ensure upload directory exists and owned by tinyhis (if present as mounted volume)
mkdir -p /app/uploads
chown -R tinyhis:tinyhis /app/uploads || true

# Start the app as tinyhis
exec su-exec tinyhis java $JAVA_OPTS -jar app.jar
