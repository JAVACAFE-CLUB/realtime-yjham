package com.realtime.cleansingsystem.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "com.realtime.cleansingsystem")
public class MongoConfig {

    @Value("${spring.data.mongodb.host}")
    private String host;

    @Value("${spring.data.mongodb.port}")
    private int port;

    @Value("${spring.data.mongodb.database}")
    private String collectionDatabase;

    @Value("${cleansing.mongodb.database}")
    private String cleansingDatabase;

    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create(String.format("mongodb://%s:%d", host, port));
    }

    /**
     * 원본 데이터 조회용 MongoTemplate (Primary)
     * collection-system에서 저장한 데이터 조회
     * Repository들이 기본적으로 이 템플릿을 사용
     */
    @Primary
    @Bean(name = "mongoTemplate")
    public MongoTemplate mongoTemplate(MongoClient mongoClient) {
        return new MongoTemplate(mongoClient, collectionDatabase);
    }

    /**
     * 정제 데이터 저장용 MongoTemplate
     * cleansing-system에서 정제한 데이터 저장
     */
    @Bean(name = "cleansingMongoTemplate")
    public MongoTemplate cleansingMongoTemplate(MongoClient mongoClient) {
        return new MongoTemplate(mongoClient, cleansingDatabase);
    }
}
