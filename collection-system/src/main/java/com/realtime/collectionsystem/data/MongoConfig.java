package com.realtime.collectionsystem.data;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "com.realtime.collectionsystem.*.repository")
@EnableMongoAuditing
public class MongoConfig {
}
