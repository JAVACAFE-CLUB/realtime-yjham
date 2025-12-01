package com.realtime.trend.processing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Kafka 토픽 설정
 */
@ConfigurationProperties(prefix = "kafka.topics")
public record KafkaTopicProperties(
        String rawNews,
        String rawYoutube,
        String processedNews,
        String processedYoutube,
        String dlqNews,
        String dlqYoutube
) {}
