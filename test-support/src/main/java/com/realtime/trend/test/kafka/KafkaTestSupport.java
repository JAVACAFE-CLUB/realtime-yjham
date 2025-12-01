package com.realtime.trend.test.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

public class KafkaTestSupport {

    private final String bootstrapServers;
    private final ObjectMapper objectMapper;

    public KafkaTestSupport(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * 테스트용 Kafka Producer 생성 (JavaTimeModule 적용)
     */
    public <T> KafkaProducer<String, T> createProducer() {
        JsonSerializer<T> jsonSerializer = new JsonSerializer<>(objectMapper);

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        return new KafkaProducer<>(props, new StringSerializer(), jsonSerializer);
    }

    /**
     * 테스트용 Kafka Consumer 생성 (타입 지정)
     */
    public <T> KafkaConsumer<String, T> createConsumer(String groupId, Class<T> valueType) {
        JsonDeserializer<T> jsonDeserializer = new JsonDeserializer<>(valueType, objectMapper);
        jsonDeserializer.addTrustedPackages("*");

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new KafkaConsumer<>(props, new StringDeserializer(), jsonDeserializer);
    }

    /**
     * 테스트용 Kafka Consumer 생성 (Map으로 수신 - 실제 앱과 동일한 방식)
     * 실제 processing-module, indexing-module과 동일하게 Map<String, Object>로 역직렬화
     */
    @SuppressWarnings("unchecked")
    public KafkaConsumer<String, Map<String, Object>> createMapConsumer(String groupId) {
        JsonDeserializer<Object> jsonDeserializer = new JsonDeserializer<>(Object.class, objectMapper);
        jsonDeserializer.addTrustedPackages("*");
        jsonDeserializer.setUseTypeHeaders(false);

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new KafkaConsumer<>(props, new StringDeserializer(), (JsonDeserializer) jsonDeserializer);
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
     * 메시지 발행 (Map 형태로 직접 발행 - LocalDateTime 배열 형식 테스트용)
     */
    public void sendMessageAsMap(String topic, String key, Map<String, Object> value) {
        try (KafkaProducer<String, Map<String, Object>> producer = createProducer()) {
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
     * 토픽에서 Map 형태로 메시지 수신 (실제 앱과 동일한 방식)
     */
    public List<Map<String, Object>> consumeMessagesAsMap(String topic, String groupId,
                                                          int expectedCount, Duration timeout) {
        List<Map<String, Object>> messages = new ArrayList<>();

        try (KafkaConsumer<String, Map<String, Object>> consumer = createMapConsumer(groupId)) {
            consumer.subscribe(Collections.singletonList(topic));

            await()
                .atMost(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> {
                    ConsumerRecords<String, Map<String, Object>> records = consumer.poll(Duration.ofMillis(100));
                    for (ConsumerRecord<String, Map<String, Object>> record : records) {
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

    /**
     * 토픽에서 첫 번째 메시지를 Map으로 수신
     */
    public Map<String, Object> consumeFirstMessageAsMap(String topic, String groupId, Duration timeout) {
        List<Map<String, Object>> messages = consumeMessagesAsMap(topic, groupId, 1, timeout);
        return messages.isEmpty() ? null : messages.get(0);
    }

    /**
     * ObjectMapper 반환 (테스트에서 직접 사용 가능)
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
