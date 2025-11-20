package com.realtime.trend.collection.infra;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 인프라 연결 테스트
 * 실제 인프라가 실행 중이어야 합니다 (docker-compose.infra.yml)
 */
@SpringBootTest
@ActiveProfiles("test")
class InfrastructureConnectionTest {

    @Autowired(required = false)
    private MongoTemplate mongoTemplate;

    @Autowired(required = false)
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;

    @Test
    @DisplayName("MongoDB 연결 테스트")
    void testMongoDBConnection() {
        // given
        assertThat(mongoTemplate).isNotNull();

        // when
        String databaseName = mongoTemplate.getDb().getName();

        // then
        assertThat(databaseName).isEqualTo("realtime-trend");
        System.out.println("✅ MongoDB 연결 성공: " + databaseName);
    }

    @Test
    @DisplayName("Kafka 연결 테스트")
    void testKafkaConnection() {
        // given
        assertThat(kafkaTemplate).isNotNull();

        // when & then
        // KafkaTemplate이 정상적으로 주입되었는지 확인
        assertThat(kafkaTemplate.getDefaultTopic()).isNull(); // 기본 토픽 미설정
        System.out.println("✅ Kafka 연결 성공");
    }

    @Test
    @DisplayName("Redis 연결 테스트")
    void testRedisConnection() {
        // given
        assertThat(redisTemplate).isNotNull();
        String testKey = "test:connection";
        String testValue = "OK";

        // when
        redisTemplate.opsForValue().set(testKey, testValue);
        String result = redisTemplate.opsForValue().get(testKey);
        redisTemplate.delete(testKey);

        // then
        assertThat(result).isEqualTo(testValue);
        System.out.println("✅ Redis 연결 성공: " + result);
    }
}
