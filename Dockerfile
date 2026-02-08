# Multi-stage build for Spring Boot application
# This Dockerfile is at the root because Railway builds from repository root
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Copy Maven wrapper and pom.xml from backend directory
COPY backend/mvnw ./backend/
COPY backend/.mvn ./backend/.mvn
COPY backend/pom.xml ./backend/

# Make mvnw executable
RUN chmod +x ./backend/mvnw

# Download dependencies (cached layer)
WORKDIR /app/backend
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY backend/src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/backend/target/appointmentscheduler-0.0.1-SNAPSHOT.jar app.jar

# Expose port (Railway will set PORT env var)
EXPOSE 8080

# Run the application
# Railway sets PORT automatically, Spring Boot will read it from environment
CMD ["java", "-jar", "app.jar"]
