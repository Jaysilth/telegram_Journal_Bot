FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
# Copy wrapper and resolve dependencies (cache layer)
COPY pom.xml mvnw mvnw.cmd ./.mvn ./ \
  && chmod +x mvnw mvnw.cmd
RUN ./mvnw dependency:go-offline -B
# Copy source and build
COPY src ./src
RUN chmod +x mvnw mvnw.cmd && ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copy JAR
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=8080"]

