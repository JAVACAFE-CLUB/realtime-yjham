# Multi-stage build for indexing-module
FROM gradle:8.5-jdk21 AS builder

WORKDIR /app

# Copy gradle wrapper and build files first (for caching)
COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./

# Copy subproject build files
COPY indexing-module/build.gradle.kts ./indexing-module/
COPY test-support/build.gradle.kts ./test-support/

# Copy source code
COPY indexing-module/src ./indexing-module/src
COPY test-support/src ./test-support/src

# Build the application (skip tests)
RUN gradle :indexing-module:bootJar -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built JAR
COPY --from=builder /app/indexing-module/build/libs/*-SNAPSHOT.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
