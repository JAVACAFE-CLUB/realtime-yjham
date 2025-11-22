# Multi-stage build
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Gradle wrapper 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

# 모듈 빌드 파일 복사
COPY processing-module/build.gradle.kts processing-module/
COPY processing-module/src processing-module/src

# 빌드
RUN chmod +x ./gradlew
RUN ./gradlew :processing-module:bootJar -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 빌드된 JAR 복사
COPY --from=builder /app/processing-module/build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8082

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
