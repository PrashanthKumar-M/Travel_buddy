# Multi-stage Docker build for Travel Buddy Spring Boot API
# Stage 1: Build distribution using Gradle & JDK 21
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace

# Copy gradle wrapper and project configurations
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY backend/build.gradle.kts ./backend/
COPY backend/src ./backend/src

# Build distribution
RUN chmod +x gradlew
RUN ./gradlew :backend:installDist --no-daemon

# Stage 2: Production JRE image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy installed distribution
COPY --from=builder /workspace/backend/build/install/backend /app

# Render passes $PORT dynamically
ENV PORT=8080
EXPOSE ${PORT}

# Healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD wget -q --spider http://localhost:${PORT}/api/health || exit 1

ENTRYPOINT ["/app/bin/backend"]
