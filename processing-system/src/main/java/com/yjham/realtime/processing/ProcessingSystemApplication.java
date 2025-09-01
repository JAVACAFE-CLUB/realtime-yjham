package com.yjham.realtime.processing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafkaStreams;

@SpringBootApplication
@EnableKafkaStreams
public class ProcessingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProcessingSystemApplication.class, args);
    }
}