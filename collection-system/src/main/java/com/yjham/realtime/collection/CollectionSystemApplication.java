package com.yjham.realtime.collection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CollectionSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(CollectionSystemApplication.class, args);
    }
}