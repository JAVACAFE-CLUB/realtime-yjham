package com.realtime.trend.processing.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

@Configuration
public class KafkaConfig {

    @Value("${kafka.topics.processed-news}")
    private String processedNewsTopic;

    @Value("${kafka.topics.processed-youtube}")
    private String processedYoutubeTopic;

    @Value("${kafka.topics.dlq-news}")
    private String dlqNewsTopic;

    @Value("${kafka.topics.dlq-youtube}")
    private String dlqYoutubeTopic;

    @Value("${processing.retry.max-attempts}")
    private int maxRetryAttempts;

    @Value("${processing.retry.initial-interval}")
    private long initialInterval;

    @Value("${processing.retry.multiplier}")
    private double multiplier;

    @Bean
    public NewTopic processedNewsTopic() {
        return TopicBuilder.name(processedNewsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic processedYoutubeTopic() {
        return TopicBuilder.name(processedYoutubeTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic dlqNewsTopic() {
        return TopicBuilder.name(dlqNewsTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic dlqYoutubeTopic() {
        return TopicBuilder.name(dlqYoutubeTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        // 에러 핸들러 설정
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(maxRetryAttempts);
        backOff.setInitialInterval(initialInterval);
        backOff.setMultiplier(multiplier);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(backOff);
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}
