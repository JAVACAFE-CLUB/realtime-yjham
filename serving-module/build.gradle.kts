dependencies {
    // Web
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Elasticsearch
    implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")

    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Cache - Caffeine
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    // Rate Limiting
    implementation("com.bucket4j:bucket4j_jdk17-core:8.15.0")
}
