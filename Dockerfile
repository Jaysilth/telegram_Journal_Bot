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
RUN find . -name "*.jar" -type f -exec sh -c 'cp "$1" app.jar' _ {} \; && \
    ls -la app.jar && echo "✅ JAR ready: $(du -h app.jar)"

# Security
RUN addgroup -g 1001 spring && adduser -S springuser -u 1001 -G spring
USER springuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

# syntax=docker/dockerfile:1
FROM eclipse