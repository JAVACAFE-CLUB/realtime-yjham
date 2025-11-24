package com.realtime.trend.test.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/**
 * Elasticsearch + Redis 통합 테스트 기본 클래스 (serving-module용)
 */
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestcontainersConfig.ElasticsearchRedisInitializer.class)
public abstract class AbstractElasticsearchRedisIntegrationTest {
}
