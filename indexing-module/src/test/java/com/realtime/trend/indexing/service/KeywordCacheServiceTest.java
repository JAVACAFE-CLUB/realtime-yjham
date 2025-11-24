package com.realtime.trend.indexing.service;

import com.realtime.trend.indexing.config.CacheConfig;
import com.realtime.trend.indexing.dto.KeywordAggregation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeywordCacheServiceTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    private KeywordCacheService keywordCacheService;

    @BeforeEach
    void setUp() {
        keywordCacheService = new KeywordCacheService(cacheManager);
    }

    @Test
    @DisplayName("집계 결과를 캐시에 저장한다")
    void cacheAggregations_savesToCache() {
        // given
        when(cacheManager.getCache(CacheConfig.KEYWORDS_CACHE)).thenReturn(cache);

        List<KeywordAggregation> newsAll = List.of(
                new KeywordAggregation("삼성전자", "ORG", 10),
                new KeywordAggregation("이재용", "PER", 5)
        );
        List<KeywordAggregation> youtubeAll = List.of(
                new KeywordAggregation("BTS", "ORG", 20)
        );

        Map<String, List<KeywordAggregation>> aggregations = Map.of(
                "news:all", newsAll,
                "youtube:all", youtubeAll
        );

        // when
        keywordCacheService.cacheAggregations(aggregations);

        // then
        verify(cache).put("news:all", newsAll);
        verify(cache).put("youtube:all", youtubeAll);
    }

    @Test
    @DisplayName("캐시가 없으면 저장하지 않는다")
    void cacheAggregations_whenCacheNotFound_doesNotSave() {
        // given
        when(cacheManager.getCache(CacheConfig.KEYWORDS_CACHE)).thenReturn(null);

        Map<String, List<KeywordAggregation>> aggregations = Map.of(
                "news:all", List.of(new KeywordAggregation("test", "ORG", 1))
        );

        // when
        keywordCacheService.cacheAggregations(aggregations);

        // then
        verify(cache, never()).put(anyString(), any());
    }

    @Test
    @DisplayName("빈 집계 결과도 캐시에 저장한다")
    void cacheAggregations_withEmptyAggregations_savesEmpty() {
        // given
        when(cacheManager.getCache(CacheConfig.KEYWORDS_CACHE)).thenReturn(cache);

        Map<String, List<KeywordAggregation>> aggregations = Map.of();

        // when
        keywordCacheService.cacheAggregations(aggregations);

        // then
        verify(cache, never()).put(anyString(), any());
    }
}
