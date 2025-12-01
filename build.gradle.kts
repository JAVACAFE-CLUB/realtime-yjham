plugins {
    id("java")
    id("org.springframework.boot") version "3.3.7" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("com.google.protobuf") version "0.9.4" apply false
}

allprojects {
    group = "com.realtime.trend"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    dependencies {
        // Spring Boot Starters
        implementation("org.springframework.boot:spring-boot-starter-web")
        implementation("org.springframework.boot:spring-boot-starter-actuator")
        implementation("org.springframework.boot:spring-boot-starter-validation")

        // MongoDB
        implementation("org.springframework.boot:spring-boot-starter-data-mongodb")

        // Kafka
        implementation("org.springframework.kafka:spring-kafka")

        // Redis
        implementation("org.springframework.boot:spring-boot-starter-data-redis")
        implementation("org.springframework.boot:spring-boot-starter-cache")

        // Monitoring
        implementation("io.micrometer:micrometer-registry-prometheus")

        // Lombok
        implementation("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")

        // Jackson
        implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

        // Apache Commons
        implementation("org.apache.commons:commons-lang3")

        // 테스트
        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testImplementation("org.springframework.kafka:spring-kafka-test")
        testImplementation("org.testcontainers:testcontainers:1.20.4")
        testImplementation("org.testcontainers:junit-jupiter:1.20.4")
        testImplementation("org.testcontainers:mongodb:1.20.4")
        testImplementation("org.testcontainers:kafka:1.20.4")
        testImplementation("org.testcontainers:elasticsearch:1.20.4")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
