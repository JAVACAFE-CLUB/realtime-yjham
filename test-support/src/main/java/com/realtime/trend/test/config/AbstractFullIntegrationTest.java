package com.realtime.trend.test.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/**
 * 전체 인프라 통합 테스트 기본 클래스
 * MongoDB, Kafka, Elasticsearch, Redis 모두 사용
 */
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestcontainersConfig.AllContainersInitializer.class)
public abstract class AbstractFullIntegrationTest {
}
