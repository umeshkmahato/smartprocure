# =============================================================================
# SmartProcure Backend – Multi-Stage Docker Build
# Java 21 LTS  |  Spring Boot 4.0.x  |  Non-root runtime
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1: Build  (Maven 3.9 + JDK 21)
# Dependency layer is cached separately from source — only invalidated when
# pom.xml changes, not every code change.
# -----------------------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# 1. Copy POM and download dependencies (cacheable layer)
COPY pom.xml .
RUN mvn dependency:go-offline -q

# 2. Copy source and build the executable JAR (skip tests – run separately)
COPY src ./src
RUN mvn clean package -DskipTests -q

# -----------------------------------------------------------------------------
# Stage 2: Runtime  (JRE 21 – minimal attack surface)
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

# Security: run as a dedicated non-root user
RUN addgroup -S smartprocure && adduser -S smartprocure -G smartprocure

WORKDIR /app

# Copy the packaged JAR from the build stage
COPY --from=builder /build/target/*.jar app.jar

# Create required directories and transfer ownership
RUN mkdir -p /app/uploads/invoices /app/logs \
    && chown -R smartprocure:smartprocure /app

USER smartprocure

EXPOSE 8080

# Actuator-based liveness probe
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# JVM flags optimised for containerised workloads
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dfile.encoding=UTF-8", \
    "-jar", "app.jar"]
