package com.realtime.trend.test.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/**
 * Kafka 통합 테스트 기본 클래스
 */
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestcontainersConfig.KafkaInitializer.class)
public abstract class AbstractKafkaIntegrationTest {
}
