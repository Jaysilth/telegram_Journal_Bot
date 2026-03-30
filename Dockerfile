# Build stage
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# Copy wrapper and resolve dependencies (cache layer)
COPY pom.xml .
COPY mvnw mvnw.cmd ./
COPY .mvn ./.mvn
RUN chmod +x mvnw mvnw.cmd

# Cache dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Production stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built jar
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]