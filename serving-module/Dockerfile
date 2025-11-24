# Simple JAR copy approach (pre-built locally)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 로컬에서 빌드된 JAR 복사 (./gradlew build 먼저 실행 필요)
COPY serving-module/build/libs/*-SNAPSHOT.jar app.jar

# 포트 노출
EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
