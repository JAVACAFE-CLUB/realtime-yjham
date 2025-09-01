plugins {
    java
    id("org.springframework.boot") version "3.1.2" apply false
    id("io.spring.dependency-management") version "1.1.2" apply false
}

allprojects {
    group = "com.yjham.realtime"
    version = "1.0.0"
    
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
        implementation("com.fasterxml.jackson.core:jackson-databind")
        
        testImplementation("org.springframework.boot:spring-boot-starter-test")
    }
    
    tasks.test {
        useJUnitPlatform()
    }
    
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-Xlint:all")) // 추후 소나 큐브로 변경 예정
    }
}