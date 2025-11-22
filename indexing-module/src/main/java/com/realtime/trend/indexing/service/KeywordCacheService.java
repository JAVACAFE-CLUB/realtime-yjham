package com.realtime.trend.indexing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.realtime.trend.indexing.dto.KeywordAggregation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class KeywordCacheService {

    private static final Logger log = LoggerFactory.getLogger(KeywordCacheService.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${cache.ttl-minutes}")
    private int ttlMinutes;

    @Value("${cache.key-prefix}")
    private String keyPrefix;

    public KeywordCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void cacheAggregations(Map<String, List<KeywordAggregation>> aggregations) {
        aggregations.forEach((key, value) -> {
            String[] parts = key.split(":");
            String source = parts[0];
            String type = parts[1];
            cacheKeywords(source, type, value);
        });
        log.info("키워드 캐시 저장 완료: {}개 조합", aggregations.size());
    }

    public void cacheKeywords(String source, String type, List<KeywordAggregation> keywords) {
        String cacheKey = buildCacheKey(source, type);
        try {
            String json = objectMapper.writeValueAsString(keywords);
            redisTemplate.opsForValue().set(cacheKey, json, Duration.ofMinutes(ttlMinutes));
            log.debug("캐시 저장: {} - {}개 키워드", cacheKey, keywords.size());
        } catch (JsonProcessingException e) {
            log.error("캐시 저장 실패: {}", cacheKey, e);
        }
    }

    public List<KeywordAggregation> getKeywords(String source, String type) {
        String cacheKey = buildCacheKey(source, type);
        try {
            String json = redisTemplate.opsForValue().get(cacheKey);
            if (json != null) {
                return objectMapper.readValue(json,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, KeywordAggregation.class));
            }
        } catch (JsonProcessingException e) {
            log.error("캐시 조회 실패: {}", cacheKey, e);
        }
        return null;
    }

    private String buildCacheKey(String source, String type) {
        return String.format("%s:source:%s:type:%s", keyPrefix, source, type);
    }
}
