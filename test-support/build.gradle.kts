plugins {
    id("java-library")
}

// test-support는 Spring Boot 애플리케이션이 아니므로 bootJar 비활성화
tasks.named("bootJar") {
    enabled = false
}

tasks.named<Jar>("jar") {
    enabled = true
}

dependencies {
    // Spring Boot Test
    api("org.springframework.boot:spring-boot-starter-test")
    api("org.springframework.boot:spring-boot-testcontainers")

    // Testcontainers
    api("org.testcontainers:testcontainers:1.20.4")
    api("org.testcontainers:junit-jupiter:1.20.4")
    api("org.testcontainers:mongodb:1.20.4")
    api("org.testcontainers:kafka:1.20.4")
    api("org.testcontainers:elasticsearch:1.20.4")

    // Kafka Test
    api("org.springframework.kafka:spring-kafka-test")

    // Spring Data (for test fixtures)
    api("org.springframework.boot:spring-boot-starter-data-mongodb")
    api("org.springframework.data:spring-data-elasticsearch")

    // Awaitility for async testing
    api("org.awaitility:awaitility:4.2.0")

    // AssertJ
    api("org.assertj:assertj-core")
}
