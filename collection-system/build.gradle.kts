dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Spring Batch
    implementation("org.springframework.boot:spring-boot-starter-batch")

    // MySQL (Spring Batch 메타데이터)
    runtimeOnly("com.mysql:mysql-connector-j")

    // MongoDB (수집 데이터 저장)
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // Jsoup (HTML 파싱)
    implementation("org.jsoup:jsoup:1.17.2")

    // YouTube Data API
    implementation("com.google.apis:google-api-services-youtube:v3-rev20240814-2.0.0")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.23.0")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.batch:spring-batch-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")

    // Testcontainers
    testImplementation("org.testcontainers:testcontainers:1.19.8")
    testImplementation("org.testcontainers:junit-jupiter:1.19.8")
    testImplementation("org.testcontainers:mongodb:1.19.8")
    testImplementation("org.testcontainers:mysql:1.19.8")
    testImplementation("org.testcontainers:kafka:1.19.8")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
