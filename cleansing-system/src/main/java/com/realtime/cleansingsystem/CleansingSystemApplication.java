package com.realtime.cleansingsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication
public class CleansingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(CleansingSystemApplication.class, args);
    }

}
