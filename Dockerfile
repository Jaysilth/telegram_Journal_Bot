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

# Production stage (minimal runtime image)
FROM eclipse-temurin:21-jre-alpine AS production
WORKDIR /app

# Copy the built JAR file
COPY --from=builder /app/target/*.jar app.jar

# Add non-root user for security (Alpine compatible)
RUN addgroup -g 1001 -S spring && \
    adduser -S springuser -u 1001 -G spring
USER springuser

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]