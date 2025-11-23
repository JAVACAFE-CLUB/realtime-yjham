package com.realtime.trend.indexing.service;

import com.realtime.trend.indexing.config.CacheConfig;
import com.realtime.trend.indexing.dto.KeywordAggregation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("KeywordCacheService 테스트")
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

    @Nested
    @DisplayName("cacheAggregations 메서드")
    class CacheAggregationsMethod {

        @Test
        @DisplayName("모든 집계 결과를 캐시에 저장")
        void shouldCacheAllAggregations() {
            // given
            when(cacheManager.getCache(CacheConfig.KEYWORDS_CACHE)).thenReturn(cache);

            Map<String, List<KeywordAggregation>> aggregations = Map.of(
                    "all:all", createKeywordAggregations(),
                    "news:all", createKeywordAggregations()
            );

            // when
            keywordCacheService.cacheAggregations(aggregations);

            // then
            verify(cache, times(2)).put(any(), any());
        }

        @Test
        @DisplayName("캐시를 찾을 수 없으면 저장하지 않음")
        void shouldNotCacheWhenCacheNotFound() {
            // given
            when(cacheManager.getCache(CacheConfig.KEYWORDS_CACHE)).thenReturn(null);

            Map<String, List<KeywordAggregation>> aggregations = Map.of(
                    "all:all", createKeywordAggregations()
            );

            // when
            keywordCacheService.cacheAggregations(aggregations);

            // then
            verify(cache, never()).put(any(), any());
        }
    }

    @Nested
    @DisplayName("cacheKeywords 메서드")
    class CacheKeywordsMethod {

        @Test
        @DisplayName("키워드를 캐시에 저장하고 반환")
        void shouldCacheAndReturnKeywords() {
            // given
            List<KeywordAggregation> keywords = createKeywordAggregations();

            // when
            List<KeywordAggregation> result = keywordCacheService.cacheKeywords("news", "PERSON", keywords);

            // then
            assertThat(result).isEqualTo(keywords);
        }
    }

    @Nested
    @DisplayName("getKeywords 메서드")
    class GetKeywordsMethod {

        @Test
        @DisplayName("캐시 미스 시 null 반환")
        void shouldReturnNullOnCacheMiss() {
            // when
            List<KeywordAggregation> result = keywordCacheService.getKeywords("news", "PERSON");

            // then
            assertThat(result).isNull();
        }
    }

    private List<KeywordAggregation> createKeywordAggregations() {
        return List.of(
                new KeywordAggregation("삼성전자", "ORGANIZATION", 100),
                new KeywordAggregation("서울", "LOCATION", 80)
        );
    }
}
