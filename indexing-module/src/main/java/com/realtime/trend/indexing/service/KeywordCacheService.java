package com.realtime.trend.indexing.service;

import com.realtime.trend.indexing.config.CacheConfig;
import com.realtime.trend.indexing.dto.KeywordAggregation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class KeywordCacheService {

    private static final Logger log = LoggerFactory.getLogger(KeywordCacheService.class);

    private final CacheManager cacheManager;

    public KeywordCacheService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * 모든 집계 결과를 캐시에 저장
     */
    public void cacheAggregations(Map<String, List<KeywordAggregation>> aggregations) {
        Cache cache = cacheManager.getCache(CacheConfig.KEYWORDS_CACHE);
        if (cache == null) {
            log.warn("캐시를 찾을 수 없음: {}", CacheConfig.KEYWORDS_CACHE);
            return;
        }

        aggregations.forEach((key, value) -> {
            cache.put(key, value);
            log.debug("캐시 저장: {} - {}개 키워드", key, value.size());
        });

        log.info("키워드 캐시 저장 완료: {}개 조합", aggregations.size());
    }

    /**
     * 모든 집계 결과를 캐시에 저장 (category 포함)
     */
    public void cacheAggregationsWithCategory(Map<String, List<KeywordAggregation>> aggregations) {
        Cache cache = cacheManager.getCache(CacheConfig.KEYWORDS_CACHE);
        if (cache == null) {
            log.warn("캐시를 찾을 수 없음: {}", CacheConfig.KEYWORDS_CACHE);
            return;
        }

        aggregations.forEach((key, value) -> {
            cache.put(key, value);
            log.debug("캐시 저장: {} - {}개 키워드", key, value.size());
        });

        log.info("키워드 캐시 저장 완료 (카테고리 포함): {}개 조합", aggregations.size());
    }

    /**
     * 특정 소스/타입/카테고리 조합의 키워드를 캐시에 저장
     */
    @CachePut(value = CacheConfig.KEYWORDS_CACHE, key = "#source + ':' + #type + ':' + #category")
    public List<KeywordAggregation> cacheKeywords(String source, String type, String category, List<KeywordAggregation> keywords) {
        log.debug("캐시 저장: {}:{}:{} - {}개 키워드", source, type, category, keywords.size());
        return keywords;
    }

    /**
     * 캐시에서 키워드 조회
     */
    @Cacheable(value = CacheConfig.KEYWORDS_CACHE, key = "#source + ':' + #type + ':' + #category", unless = "#result == null")
    public List<KeywordAggregation> getKeywords(String source, String type, String category) {
        log.debug("캐시 미스: {}:{}:{}", source, type, category);
        return null;
    }

    /**
     * 특정 캐시 키 삭제
     */
    @CacheEvict(value = CacheConfig.KEYWORDS_CACHE, key = "#source + ':' + #type + ':' + #category")
    public void evictKeywords(String source, String type, String category) {
        log.debug("캐시 삭제: {}:{}:{}", source, type, category);
    }

    /**
     * 전체 캐시 삭제
     */
    @CacheEvict(value = CacheConfig.KEYWORDS_CACHE, allEntries = true)
    public void evictAllKeywords() {
        log.info("전체 키워드 캐시 삭제");
    }
}
