dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("co.elastic.clients:elasticsearch-java:8.8.2")
    
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.testcontainers:elasticsearch")
    testImplementation("org.testcontainers:junit-jupiter")
}