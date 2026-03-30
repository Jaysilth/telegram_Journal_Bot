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

# Production stage - BULLETPROOF JAR COPY
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 🔥 FIX 1: Copy ALL target contents
COPY --from=builder /app/target/ ./target/

# 🔥 FIX 2: Find & copy the FIRST executable JAR
RUN find . -name "*.jar" -type f -exec sh -c 'cp "$1" app.jar && echo "✅ COPIED: $1"' _ {} \; && \
    ls -la app.jar && \
    java -jar app.jar --version

# Security user
RUN addgroup -g 1001 spring && adduser -S springuser -u 1001 -G spring
USER springuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]