package com.realtime.trend.serving.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RateLimitConfig 단위 테스트")
class RateLimitConfigTest {

    private RateLimitConfig rateLimitConfig;

    @BeforeEach
    void setUp() {
        rateLimitConfig = new RateLimitConfig();
        ReflectionTestUtils.setField(rateLimitConfig, "requestsPerMinute", 10);
        ReflectionTestUtils.setField(rateLimitConfig, "requestsPerHour", 100);
    }

    @Test
    @DisplayName("첫 요청은 허용되어야 한다")
    void tryConsume_firstRequest_shouldAllow() {
        // given
        String clientIp = "192.168.1.1";

        // when
        boolean result = rateLimitConfig.tryConsume(clientIp);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("분당 요청 제한 초과 시 요청이 거부되어야 한다")
    void tryConsume_exceedsPerMinuteLimit_shouldDeny() {
        // given
        String clientIp = "192.168.1.2";

        // when - 분당 10개 요청 소진
        for (int i = 0; i < 10; i++) {
            rateLimitConfig.tryConsume(clientIp);
        }
        boolean result = rateLimitConfig.tryConsume(clientIp);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("서로 다른 IP는 독립적인 버킷을 사용해야 한다")
    void tryConsume_differentIps_shouldHaveSeparateBuckets() {
        // given
        String clientIp1 = "192.168.1.1";
        String clientIp2 = "192.168.1.2";

        // when - IP1의 요청 제한 소진
        for (int i = 0; i < 10; i++) {
            rateLimitConfig.tryConsume(clientIp1);
        }

        // then - IP2는 여전히 요청 가능
        assertThat(rateLimitConfig.tryConsume(clientIp1)).isFalse();
        assertThat(rateLimitConfig.tryConsume(clientIp2)).isTrue();
    }

    @Test
    @DisplayName("resolveBucket은 동일 IP에 대해 같은 버킷을 반환해야 한다")
    void resolveBucket_sameIp_shouldReturnSameBucket() {
        // given
        String clientIp = "192.168.1.1";

        // when
        var bucket1 = rateLimitConfig.resolveBucket(clientIp);
        var bucket2 = rateLimitConfig.resolveBucket(clientIp);

        // then
        assertThat(bucket1).isSameAs(bucket2);
    }

    @Test
    @DisplayName("getAvailableTokens은 남은 토큰 수를 반환해야 한다")
    void getAvailableTokens_shouldReturnRemainingTokens() {
        // given
        String clientIp = "192.168.1.1";

        // when
        long initialTokens = rateLimitConfig.getAvailableTokens(clientIp);
        rateLimitConfig.tryConsume(clientIp);
        long afterConsumeTokens = rateLimitConfig.getAvailableTokens(clientIp);

        // then
        assertThat(initialTokens).isEqualTo(10);  // 분당 제한이 더 작으므로
        assertThat(afterConsumeTokens).isEqualTo(9);
    }
}
