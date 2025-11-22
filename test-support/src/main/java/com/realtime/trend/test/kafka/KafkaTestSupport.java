package com.realtime.trend.test.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

/**
 * Kafka 테스트 지원 유틸리티
 */
public class KafkaTestSupport {

    private final String bootstrapServers;

    public KafkaTestSupport(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    /**
     * 테스트용 Kafka Producer 생성
     */
    public <T> KafkaProducer<String, T> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new KafkaProducer<>(props);
    }

    /**
     * 테스트용 Kafka Consumer 생성
     */
    public <T> KafkaConsumer<String, T> createConsumer(String groupId, Class<T> valueType) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, valueType.getName());
        return new KafkaConsumer<>(props);
    }

    /**
     * 메시지 발행
     */
    public <T> void sendMessage(String topic, String key, T value) {
        try (KafkaProducer<String, T> producer = createProducer()) {
            producer.send(new ProducerRecord<>(topic, key, value));
            producer.flush();
        }
    }

    /**
     * 토픽에서 메시지 수신 (타임아웃 적용)
     */
    public <T> List<T> consumeMessages(String topic, String groupId, Class<T> valueType,
                                        int expectedCount, Duration timeout) {
        List<T> messages = new ArrayList<>();

        try (KafkaConsumer<String, T> consumer = createConsumer(groupId, valueType)) {
            consumer.subscribe(Collections.singletonList(topic));

            await()
                .atMost(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> {
                    ConsumerRecords<String, T> records = consumer.poll(Duration.ofMillis(100));
                    for (ConsumerRecord<String, T> record : records) {
                        messages.add(record.value());
                    }
                    return messages.size() >= expectedCount;
                });
        }

        return messages;
    }

    /**
     * 토픽에서 첫 번째 메시지 수신
     */
    public <T> T consumeFirstMessage(String topic, String groupId, Class<T> valueType, Duration timeout) {
        List<T> messages = consumeMessages(topic, groupId, valueType, 1, timeout);
        return messages.isEmpty() ? null : messages.get(0);
    }
}
