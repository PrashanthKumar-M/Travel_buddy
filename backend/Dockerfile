# Multi-stage Docker build for Travel Buddy Spring Boot API
# Stage 1: Build distribution using pre-installed Gradle & JDK 21
FROM gradle:8.10-jdk21-alpine AS builder
WORKDIR /app
COPY . .
RUN echo 'rootProject.name = "backend"' > settings.gradle.kts
RUN gradle installDist -x test --no-daemon

# Stage 2: Production JRE 21 Alpine image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy installed distribution
COPY --from=builder /app/build/install/backend /app

# Render passes $PORT dynamically
ENV PORT=8080
EXPOSE ${PORT}

# Healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD wget -q --spider http://localhost:${PORT}/api/health || exit 1

ENTRYPOINT ["/app/bin/backend"]
