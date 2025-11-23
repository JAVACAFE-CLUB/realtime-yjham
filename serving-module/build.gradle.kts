dependencies {
    // Web
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Elasticsearch
    implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")

    // Redis + Cache
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // Rate Limiting (Caffeine for bucket storage, Bucket4j for token bucket)
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation("com.bucket4j:bucket4j_jdk17-core:8.15.0")
}
