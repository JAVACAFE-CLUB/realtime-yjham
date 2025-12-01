package com.realtime.trend.indexing.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.realtime.trend.indexing.dto.KeywordAggregation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class KeywordAggregationService {

    private static final Logger log = LoggerFactory.getLogger(KeywordAggregationService.class);

    private final ElasticsearchClient elasticsearchClient;

    @Value("${elasticsearch.index.keywords}")
    private String indexName;

    @Value("${aggregation.top-count}")
    private int topCount;

    public KeywordAggregationService(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    public List<KeywordAggregation> aggregateKeywords(String source, String type, String category) {
        try {
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

            SearchResponse<Void> response = elasticsearchClient.search(s -> s
                    .index(indexName)
                    .size(0)
                    .query(q -> q
                            .bool(b -> {
                                b.must(m -> m
                                        .range(r -> r
                                                .field("collectedAt")
                                                .gte(co.elastic.clients.json.JsonData.of(startOfDay.toString()))
                                                .lte(co.elastic.clients.json.JsonData.of(endOfDay.toString()))
                                        )
                                );

                                if (source != null && !source.equals("all")) {
                                    b.must(m -> m.term(t -> t.field("source").value(source)));
                                }
                                if (type != null && !type.equals("all")) {
                                    b.must(m -> m.term(t -> t.field("type").value(type)));
                                }
                                if (category != null && !category.equals("all")) {
                                    b.must(m -> m.term(t -> t.field("category").value(category)));
                                }

                                return b;
                            })
                    )
                    .aggregations("top_keywords", a -> a
                            .terms(t -> t
                                    .field("keyword")
                                    .size(topCount)
                            )
                            .aggregations("type_info", sa -> sa
                                    .terms(st -> st
                                            .field("type")
                                            .size(1)
                                    )
                            )
                            .aggregations("category_info", sa -> sa
                                    .terms(st -> st
                                            .field("category")
                                            .size(1)
                                    )
                            )
                    ),
                    Void.class
            );

            List<KeywordAggregation> results = new ArrayList<>();
            var topKeywordsBuckets = response.aggregations()
                    .get("top_keywords")
                    .sterms()
                    .buckets()
                    .array();

            for (StringTermsBucket bucket : topKeywordsBuckets) {
                String keyword = bucket.key().stringValue();
                long count = bucket.docCount();

                String keywordType = "UNKNOWN";
                var typeInfoBuckets = bucket.aggregations()
                        .get("type_info")
                        .sterms()
                        .buckets()
                        .array();

                if (!typeInfoBuckets.isEmpty()) {
                    keywordType = typeInfoBuckets.get(0).key().stringValue();
                }

                String keywordCategory = "OTHER";
                var categoryInfoBuckets = bucket.aggregations()
                        .get("category_info")
                        .sterms()
                        .buckets()
                        .array();

                if (!categoryInfoBuckets.isEmpty()) {
                    keywordCategory = categoryInfoBuckets.get(0).key().stringValue();
                }

                results.add(new KeywordAggregation(keyword, keywordType, keywordCategory, count));
            }

            log.debug("키워드 집계 완료: source={}, type={}, category={}, count={}",
                    source, type, category, results.size());

            return results;

        } catch (Exception e) {
            log.error("키워드 집계 실패: source={}, type={}, category={}", source, type, category, e);
            return List.of();
        }
    }

    public List<KeywordAggregation> aggregateKeywords(String source, String type) {
        return aggregateKeywords(source, type, "all");
    }

    public Map<String, List<KeywordAggregation>> aggregateAll() {
        Map<String, List<KeywordAggregation>> result = new java.util.HashMap<>();

        // 전체
        result.put("all:all", aggregateKeywords("all", "all"));

        // 소스별
        result.put("news:all", aggregateKeywords("news", "all"));
        result.put("youtube:all", aggregateKeywords("youtube", "all"));

        // 타입별
        result.put("all:PERSON", aggregateKeywords("all", "PERSON"));
        result.put("all:LOCATION", aggregateKeywords("all", "LOCATION"));
        result.put("all:ORGANIZATION", aggregateKeywords("all", "ORGANIZATION"));

        // 소스+타입 조합
        result.put("news:PERSON", aggregateKeywords("news", "PERSON"));
        result.put("news:LOCATION", aggregateKeywords("news", "LOCATION"));
        result.put("news:ORGANIZATION", aggregateKeywords("news", "ORGANIZATION"));
        result.put("youtube:PERSON", aggregateKeywords("youtube", "PERSON"));
        result.put("youtube:LOCATION", aggregateKeywords("youtube", "LOCATION"));
        result.put("youtube:ORGANIZATION", aggregateKeywords("youtube", "ORGANIZATION"));

        return result;
    }
}
