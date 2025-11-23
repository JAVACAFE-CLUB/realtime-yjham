package com.realtime.trend.serving.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("CacheConfig 단위 테스트")
class CacheConfigTest {

    private CacheConfig cacheConfig;
    private CacheErrorHandler errorHandler;

    @BeforeEach
    void setUp() {
        cacheConfig = new CacheConfig();
        ReflectionTestUtils.setField(cacheConfig, "ttlMinutes", 10);
        ReflectionTestUtils.setField(cacheConfig, "keyPrefix", "test");
        errorHandler = cacheConfig.errorHandler();
    }

    @Nested
    @DisplayName("CacheErrorHandler (Graceful Degradation)")
    class CacheErrorHandlerTest {

        private Cache mockCache;
        private RuntimeException testException;

        @BeforeEach
        void setUp() {
            mockCache = mock(Cache.class);
            when(mockCache.getName()).thenReturn("testCache");
            testException = new RuntimeException("Redis connection failed");
        }

        @Test
        @DisplayName("캐시 조회 실패 시 예외를 던지지 않고 정상 처리해야 한다")
        void handleCacheGetError_shouldNotThrowException() {
            // given
            Object key = "test-key";

            // when & then - 예외가 발생하지 않아야 함 (graceful degradation)
            assertThatCode(() ->
                errorHandler.handleCacheGetError(testException, mockCache, key)
            ).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("캐시 저장 실패 시 예외를 던지지 않고 정상 처리해야 한다")
        void handleCachePutError_shouldNotThrowException() {
            // given
            Object key = "test-key";
            Object value = "test-value";

            // when & then
            assertThatCode(() ->
                errorHandler.handleCachePutError(testException, mockCache, key, value)
            ).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("캐시 삭제 실패 시 예외를 던지지 않고 정상 처리해야 한다")
        void handleCacheEvictError_shouldNotThrowException() {
            // given
            Object key = "test-key";

            // when & then
            assertThatCode(() ->
                errorHandler.handleCacheEvictError(testException, mockCache, key)
            ).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("캐시 클리어 실패 시 예외를 던지지 않고 정상 처리해야 한다")
        void handleCacheClearError_shouldNotThrowException() {
            // when & then
            assertThatCode(() ->
                errorHandler.handleCacheClearError(testException, mockCache)
            ).doesNotThrowAnyException();
        }
    }
}
