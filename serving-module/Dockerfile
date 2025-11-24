# Multi-stage build for serving-module
FROM gradle:8.5-jdk21 AS builder

WORKDIR /app

# Copy gradle wrapper and build files first (for caching)
COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./

# Copy subproject build files
COPY serving-module/build.gradle.kts ./serving-module/
COPY test-support/build.gradle.kts ./test-support/

# Copy source code
COPY serving-module/src ./serving-module/src
COPY test-support/src ./test-support/src

# Build the application (skip tests)
RUN gradle :serving-module:bootJar -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built JAR
COPY --from=builder /app/serving-module/build/libs/*-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
