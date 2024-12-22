# Start with a base image with Java
FROM openjdk:17-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the built jar into the container
COPY .env /app/.env
COPY target/demo-1.0-SNAPSHOT-jar-with-dependencies.jar /app/app.jar

# Command to run the jar
CMD ["java", "-jar", "app.jar"]