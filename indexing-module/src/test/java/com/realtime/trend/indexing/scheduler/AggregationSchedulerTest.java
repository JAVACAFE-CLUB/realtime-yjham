package com.realtime.trend.indexing.scheduler;

import com.realtime.trend.indexing.dto.KeywordAggregation;
import com.realtime.trend.indexing.service.KeywordAggregationService;
import com.realtime.trend.indexing.service.KeywordCacheService;
import com.realtime.trend.indexing.service.KeywordIndexingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("AggregationScheduler 테스트")
@ExtendWith(MockitoExtension.class)
class AggregationSchedulerTest {

    @Mock
    private KeywordAggregationService aggregationService;

    @Mock
    private KeywordCacheService cacheService;

    @Mock
    private KeywordIndexingService indexingService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Captor
    private ArgumentCaptor<AggregationScheduler.AggregatedKeywordsMessage> messageCaptor;

    private AggregationScheduler aggregationScheduler;

    @BeforeEach
    void setUp() {
        aggregationScheduler = new AggregationScheduler(
                aggregationService,
                cacheService,
                indexingService,
                kafkaTemplate
        );
        ReflectionTestUtils.setField(aggregationScheduler, "aggregatedKeywordsTopic", "aggregated-keywords-test");
    }

    @Nested
    @DisplayName("aggregateKeywords 메서드")
    class AggregateKeywordsMethod {

        @Test
        @DisplayName("집계 결과를 캐시에 저장하고 Kafka에 발행")
        void shouldCacheAndPublishAggregations() {
            // given
            List<KeywordAggregation> keywords = List.of(
                    new KeywordAggregation("삼성전자", "ORGANIZATION", 100),
                    new KeywordAggregation("서울", "LOCATION", 80)
            );
            Map<String, List<KeywordAggregation>> aggregations = Map.of(
                    "all:all", keywords,
                    "news:all", keywords
            );

            when(aggregationService.aggregateAll()).thenReturn(aggregations);

            // when
            aggregationScheduler.aggregateKeywords();

            // then
            verify(cacheService).cacheAggregations(aggregations);
            verify(kafkaTemplate).send(eq("aggregated-keywords-test"), eq("aggregated"), messageCaptor.capture());

            AggregationScheduler.AggregatedKeywordsMessage capturedMessage = messageCaptor.getValue();
            assertThat(capturedMessage.keywords()).hasSize(2);
            assertThat(capturedMessage.aggregatedAt()).isNotNull();
        }

        @Test
        @DisplayName("키워드가 없으면 Kafka 발행하지 않음")
        void shouldNotPublishWhenNoKeywords() {
            // given
            Map<String, List<KeywordAggregation>> aggregations = Map.of(
                    "all:all", List.of()
            );

            when(aggregationService.aggregateAll()).thenReturn(aggregations);

            // when
            aggregationScheduler.aggregateKeywords();

            // then
            verify(cacheService).cacheAggregations(aggregations);
            verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("집계 실패 시 예외를 삼킴")
        void shouldHandleAggregationFailure() {
            // given
            when(aggregationService.aggregateAll()).thenThrow(new RuntimeException("집계 실패"));

            // when & then (예외가 발생하지 않아야 함)
            aggregationScheduler.aggregateKeywords();

            verify(cacheService, never()).cacheAggregations(any());
        }
    }

    @Nested
    @DisplayName("cleanupOldKeywords 메서드")
    class CleanupOldKeywordsMethod {

        @Test
        @DisplayName("오래된 키워드 삭제 호출")
        void shouldCallDeleteOldKeywords() {
            // when
            aggregationScheduler.cleanupOldKeywords();

            // then
            verify(indexingService).deleteOldKeywords();
        }
    }
}
