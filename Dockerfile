# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

COPY pom.xml .
COPY mvnw mvnw.cmd ./
COPY .mvn ./.mvn
RUN chmod +x mvnw mvnw.cmd

RUN ./mvnw dependency:go-offline -B
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Production stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 🔥 PERFECT JAR COPY (already working)
COPY --from=builder /app/target/ ./target/
RUN find ./target -name "*.jar" -type f | head -1 | xargs -I {} cp {} app.jar && \
    echo "✅ JAR: $(du -h app.jar)" && \
    ls -la