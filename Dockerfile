# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# Copy Maven wrapper files first (excellent layer caching)
COPY pom.xml .
COPY mvnw mvnw.cmd ./
COPY .mvn ./.mvn
RUN chmod +x mvnw mvnw.cmd

# Cache Maven dependencies (changes only when pom.xml changes)
RUN ./mvnw dependency:go-offline -B

# Copy source code (changes frequently)
COPY src ./src

# Build the application (skip tests for faster builds)
RUN ./mvnw clean package -DskipTests

# 🔍 DEBUG: List all JARs (temporary - remove after success)
RUN find /app -name "*.jar" -ls || echo "No JARs found!"

# Production stage (minimal runtime image)
FROM eclipse-temurin:21-jre-alpine AS production
WORKDIR /app

# 🔥 BULLETPROOF JAR COPY - Works for ALL project types
COPY --from=builder /app/target/ /tmp/target/
RUN ls -la /tmp/target/ && \
    find /tmp/target -name "*.jar" | head -1 | xargs -I {} cp {} app.jar && \
    ls -la app.jar && \
    echo "✅ JAR copied successfully: $(du -h app.jar)"

# Fix permissions for non-root user
RUN chmod 644 app.jar

# Add non-root user for security (Alpine compatible)
RUN addgroup -g 1001 -S spring && \
    adduser -S springuser -u 1001 -G spring
USER springuser

# Healthcheck (Spring Boot actuator)
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || \
      wget --no-verbose --tries=1 --spider http://localhost:8080/ || exit 1

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]