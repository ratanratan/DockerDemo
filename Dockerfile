#FROM eclipse-temurin:21-jdk AS build
FROM eclipse-temurin:21-jre

# Set the working directory inside the Docker container
WORKDIR /app

# Copy the Spring Boot executable JAR from the host into the container
# Source: target/app.jar
# Destination: /app/app.jar
COPY target/SpringBootHelloWorld-0.0.1-SNAPSHOT.jar app.jar

# Execute the Spring Boot application when the container starts
ENTRYPOINT ["java", "-jar", "app.jar"]