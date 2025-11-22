package com.realtime.trend.serving.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {
    // Redis 캐싱을 사용하므로 Spring Cache는 비활성화
    // 필요 시 여기에 추가 캐시 설정 구성
}
