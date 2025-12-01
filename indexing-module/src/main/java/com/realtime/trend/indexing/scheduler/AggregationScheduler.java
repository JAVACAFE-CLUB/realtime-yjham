package com.realtime.trend.indexing.scheduler;

import com.realtime.trend.indexing.dto.KeywordAggregation;
import com.realtime.trend.indexing.service.KeywordAggregationService;
import com.realtime.trend.indexing.service.KeywordCacheService;
import com.realtime.trend.indexing.service.KeywordIndexingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class AggregationScheduler {

    private static final Logger log = LoggerFactory.getLogger(AggregationScheduler.class);

    private final KeywordAggregationService aggregationService;
    private final KeywordCacheService cacheService;
    private final KeywordIndexingService indexingService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.aggregated-keywords}")
    private String aggregatedKeywordsTopic;

    public AggregationScheduler(
            KeywordAggregationService aggregationService,
            KeywordCacheService cacheService,
            KeywordIndexingService indexingService,
            KafkaTemplate<String, Object> kafkaTemplate
    ) {
        this.aggregationService = aggregationService;
        this.cacheService = cacheService;
        this.indexingService = indexingService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(cron = "${aggregation.cron}")
    public void aggregateKeywords() {
        log.info("키워드 집계 시작");

        try {
            // 전체 조합 집계
            Map<String, List<KeywordAggregation>> allAggregations = aggregationService.aggregateAll();

            // Redis 캐시 저장
            cacheService.cacheAggregations(allAggregations);

            // Kafka 발행 (전체 키워드만)
            List<KeywordAggregation> topKeywords = allAggregations.get("all:all");
            if (topKeywords != null && !topKeywords.isEmpty()) {
                AggregatedKeywordsMessage message = new AggregatedKeywordsMessage(
                        topKeywords,
                        LocalDateTime.now()
                );
                kafkaTemplate.send(aggregatedKeywordsTopic, "aggregated", message);
                log.info("집계 결과 Kafka 발행: {}개 키워드", topKeywords.size());
            }

            log.info("키워드 집계 완료");

        } catch (Exception e) {
            log.error("키워드 집계 실패", e);
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void cleanupOldKeywords() {
        log.info("오래된 키워드 정리 시작");
        indexingService.deleteOldKeywords();
    }

    public record AggregatedKeywordsMessage(
            List<KeywordAggregation> keywords,
            LocalDateTime aggregatedAt
    ) {
    }
}
