import com.google.protobuf.gradle.*

plugins {
    id("com.google.protobuf")
}

dependencies {
    // Spring Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // gRPC
    implementation("io.grpc:grpc-netty-shaded:1.59.0")
    implementation("io.grpc:grpc-protobuf:1.59.0")
    implementation("io.grpc:grpc-stub:1.59.0")
    implementation("com.google.protobuf:protobuf-java:3.25.0")

    // gRPC용 annotation (Java 9+)
    compileOnly("org.apache.tomcat:annotations-api:6.0.53")

    // HTML 정제
    implementation("org.jsoup:jsoup:1.17.1")

    // Test
    testImplementation(project(":test-support"))
    testImplementation("org.springframework.kafka:spring-kafka-test")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.0"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.59.0"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                id("grpc")
            }
        }
    }
}
