package com.realtime.trend.serving.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.realtime.trend.serving.dto.KeywordResponse;
import com.realtime.trend.serving.dto.KeywordResponse.KeywordItem;
import com.realtime.trend.serving.dto.KeywordResponse.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class KeywordService {

    private static final Logger log = LoggerFactory.getLogger(KeywordService.class);

    private final StringRedisTemplate redisTemplate;
    private final ElasticsearchKeywordService elasticsearchService;
    private final ObjectMapper objectMapper;

    @Value("${cache.redis.key-prefix}")
    private String keyPrefix;

    @Value("${cache.redis.ttl-minutes}")
    private int ttlMinutes;

    public KeywordService(
            StringRedisTemplate redisTemplate,
            ElasticsearchKeywordService elasticsearchService,
            ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.elasticsearchService = elasticsearchService;
        this.objectMapper = objectMapper;
    }

    public KeywordResponse getKeywords(String source, String type, int limit) {
        log.debug("키워드 조회: source={}, type={}, limit={}", source, type, limit);

        // 1. Redis 캐시 조회
        List<KeywordItem> keywords = getFromRedis(source, type);

        // 2. 캐시 미스 또는 빈 데이터 시 Elasticsearch 직접 조회
        if (keywords == null || keywords.isEmpty()) {
            log.debug("Redis 캐시 미스 또는 빈 데이터, Elasticsearch 조회");
            keywords = elasticsearchService.getKeywords(source, type, limit);

            // Redis에 캐싱
            cacheToRedis(source, type, keywords);
        }

        // 3. limit 적용
        if (keywords.size() > limit) {
            keywords = keywords.subList(0, limit);
        }

        return new KeywordResponse(
                keywords,
                new Metadata(
                        keywords.size(),
                        limit,
                        source,
                        type,
                        LocalDateTime.now()
                )
        );
    }

    private List<KeywordItem> getFromRedis(String source, String type) {
        String cacheKey = buildCacheKey(source, type);
        try {
            String json = redisTemplate.opsForValue().get(cacheKey);
            if (json != null) {
                log.debug("Redis 캐시 히트: {}", cacheKey);
                return objectMapper.readValue(json,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, KeywordItem.class));
            }
        } catch (JsonProcessingException e) {
            log.error("Redis 캐시 역직렬화 실패: {}", cacheKey, e);
        }
        return null;
    }

    private void cacheToRedis(String source, String type, List<KeywordItem> keywords) {
        String cacheKey = buildCacheKey(source, type);
        try {
            String json = objectMapper.writeValueAsString(keywords);
            redisTemplate.opsForValue().set(cacheKey, json, Duration.ofMinutes(ttlMinutes));
            log.debug("Redis 캐시 저장: {}", cacheKey);
        } catch (JsonProcessingException e) {
            log.error("Redis 캐시 직렬화 실패: {}", cacheKey, e);
        }
    }

    private String buildCacheKey(String source, String type) {
        return String.format("%s:source:%s:type:%s", keyPrefix, source, type);
    }
}
