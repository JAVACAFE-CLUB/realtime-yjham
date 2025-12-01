dependencies {
    // Elasticsearch
    implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")

    // Spring Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Scheduling
    implementation("org.springframework.boot:spring-boot-starter-quartz")

    // Test
    testImplementation(project(":test-support"))
    testImplementation("org.springframework.kafka:spring-kafka-test")
}
