dependencies {
    // Spring Batch
    implementation("org.springframework.boot:spring-boot-starter-batch")

    // HTML 파싱 (Jsoup)
    implementation("org.jsoup:jsoup:1.17.1")

    // RSS 파싱 (Rome)
    implementation("com.rometools:rome:2.1.0")

    // HTTP Client
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // YouTube API
    implementation("com.google.api-client:google-api-client:2.2.0")
    implementation("com.google.apis:google-api-services-youtube:v3-rev20231011-2.0.0")

    // 테스트
    testImplementation("org.springframework.batch:spring-batch-test")
    testRuntimeOnly("com.h2database:h2")
}
