dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")

    // MongoDB (원본 데이터 조회 + 정제 데이터 저장)
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")

    // Kafka (Consumer + Producer)
    implementation("org.springframework.kafka:spring-kafka")

    // 텍스트 처리
    implementation("org.apache.commons:commons-text:1.11.0")

    // 위키 마크업 파싱
    implementation("org.sweble.wikitext:swc-engine:3.1.9")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")

    // Testcontainers
    testImplementation("org.testcontainers:testcontainers:1.19.8")
    testImplementation("org.testcontainers:junit-jupiter:1.19.8")
    testImplementation("org.testcontainers:mongodb:1.19.8")
    testImplementation("org.testcontainers:kafka:1.19.8")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
