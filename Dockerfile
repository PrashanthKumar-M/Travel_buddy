# Multi-stage Docker build for Travel Buddy Spring Boot API
# Stage 1: Build backend distribution using Gradle & JDK 21
FROM gradle:8.10-jdk21-alpine AS builder
WORKDIR /app
COPY . .
RUN gradle :backend:installDist -x test --no-daemon

# Stage 2: Production JRE 21 Alpine image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy installed backend distribution
COPY --from=builder /app/backend/build/install/backend /app

# Render passes $PORT dynamically
ENV PORT=8080
EXPOSE ${PORT}

# Healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD wget -q --spider http://localhost:${PORT}/api/health || exit 1

ENTRYPOINT ["/app/bin/backend"]
