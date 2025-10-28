package com.realtime.cleansingsystem.config;

import com.realtime.cleansingsystem.common.event.NewsCollectionEvent;
import com.realtime.cleansingsystem.common.event.WikiCollectionEvent;
import com.realtime.cleansingsystem.common.event.YoutubeCollectionEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    /**
     * Kafka Consumer 설정 (공통)
     */
    private Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    /**
     * 뉴스 Consumer Factory
     */
    @Bean
    public ConsumerFactory<String, NewsCollectionEvent> newsConsumerFactory() {
        JsonDeserializer<NewsCollectionEvent> deserializer = new JsonDeserializer<>(NewsCollectionEvent.class);
        deserializer.setUseTypeHeaders(false);
        deserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(
                consumerConfigs(),
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NewsCollectionEvent> newsKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, NewsCollectionEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(newsConsumerFactory());
        return factory;
    }

    /**
     * 위키 Consumer Factory
     */
    @Bean
    public ConsumerFactory<String, WikiCollectionEvent> wikiConsumerFactory() {
        JsonDeserializer<WikiCollectionEvent> deserializer = new JsonDeserializer<>(WikiCollectionEvent.class);
        deserializer.setUseTypeHeaders(false);
        deserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(
                consumerConfigs(),
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, WikiCollectionEvent> wikiKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, WikiCollectionEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(wikiConsumerFactory());
        return factory;
    }

    /**
     * 유튜브 Consumer Factory
     */
    @Bean
    public ConsumerFactory<String, YoutubeCollectionEvent> youtubeConsumerFactory() {
        JsonDeserializer<YoutubeCollectionEvent> deserializer = new JsonDeserializer<>(YoutubeCollectionEvent.class);
        deserializer.setUseTypeHeaders(false);
        deserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(
                consumerConfigs(),
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, YoutubeCollectionEvent> youtubeKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, YoutubeCollectionEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(youtubeConsumerFactory());
        return factory;
    }

    /**
     * Kafka Producer 설정
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
