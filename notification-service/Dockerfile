# =============================================================================
# notification-service — Production multi-stage Dockerfile
# Stage 1: Maven build  |  Stage 2: Minimal JRE 21 runtime (non-root)
# =============================================================================

# ── Stage 1: Build ───────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# Cache dependencies before copying source
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -q 2>/dev/null || true

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn package -DskipTests -q && \
    java -Djarmode=tools -jar target/notification-service-*.jar extract \
         --layers --launcher --destination extracted

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app

# Copy layered jar artifacts (improves Docker layer cache reuse)
COPY --from=builder /build/extracted/dependencies/          ./
COPY --from=builder /build/extracted/spring-boot-loader/    ./
COPY --from=builder /build/extracted/snapshot-dependencies/ ./
COPY --from=builder /build/extracted/application/           ./

USER appuser

EXPOSE 8081

# Health check — matches actuator endpoint
HEALTHCHECK --interval=20s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseZGC", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "org.springframework.boot.loader.launch.JarLauncher"]
