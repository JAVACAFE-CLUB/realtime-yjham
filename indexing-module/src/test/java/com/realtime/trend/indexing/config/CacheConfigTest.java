package com.realtime.trend.indexing.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("CacheConfig 테스트")
class CacheConfigTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private CacheConfig cacheConfig;

    @Test
    @DisplayName("RedisCacheManager가 생성됨")
    void shouldCreateRedisCacheManager() {
        assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);
    }

    @Test
    @DisplayName("keywords 캐시가 존재함")
    void shouldHaveKeywordsCache() {
        var cache = cacheManager.getCache(CacheConfig.KEYWORDS_CACHE);
        assertThat(cache).isNotNull();
    }

    @Test
    @DisplayName("CacheErrorHandler가 설정됨")
    void shouldHaveCacheErrorHandler() {
        CacheErrorHandler errorHandler = cacheConfig.errorHandler();
        assertThat(errorHandler).isNotNull();
    }

    @Test
    @DisplayName("캐시 에러 핸들러가 예외를 던지지 않음")
    void cacheErrorHandlerShouldNotThrowException() {
        CacheErrorHandler errorHandler = cacheConfig.errorHandler();
        var cache = cacheManager.getCache(CacheConfig.KEYWORDS_CACHE);

        // 에러 핸들러 메서드들이 예외를 던지지 않는지 확인
        errorHandler.handleCacheGetError(new RuntimeException("test"), cache, "key");
        errorHandler.handleCachePutError(new RuntimeException("test"), cache, "key", "value");
        errorHandler.handleCacheEvictError(new RuntimeException("test"), cache, "key");
        errorHandler.handleCacheClearError(new RuntimeException("test"), cache);
    }
}
