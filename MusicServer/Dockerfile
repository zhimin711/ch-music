# Multi-stage Dockerfile for a Maven-built Spring Boot application
# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy only the Maven files first to leverage Docker layer caching
COPY pom.xml ./

# If there are additional modules or a parent pom, adjust as needed
RUN mvn -B -DskipTests dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn -B -DskipTests package

# Runtime stage
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app
ARG JAR_FILE=target/*.jar
COPY --from=build /app/${JAR_FILE} app.jar

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["sh","-c","java -Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} --add-opens=java.base/java.lang=ALL-UNNAMED -jar /app/app.jar"]
