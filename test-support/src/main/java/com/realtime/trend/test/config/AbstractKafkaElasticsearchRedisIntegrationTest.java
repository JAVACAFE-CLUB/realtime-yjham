package com.realtime.trend.test.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/**
 * Kafka + Elasticsearch + Redis 통합 테스트 기본 클래스 (indexing-module용)
 */
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestcontainersConfig.KafkaElasticsearchRedisInitializer.class)
public abstract class AbstractKafkaElasticsearchRedisIntegrationTest {
}
