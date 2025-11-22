package com.realtime.trend.serving.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.realtime.trend.serving.dto.KeywordResponse.KeywordItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ElasticsearchKeywordService {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchKeywordService.class);

    private final ElasticsearchClient elasticsearchClient;

    @Value("${elasticsearch.index.keywords}")
    private String indexName;

    public ElasticsearchKeywordService(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public List<KeywordItem> getKeywords(String source, String type, int limit) {
        try {
            LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
            String startOfDay = today.atStartOfDay().format(DATE_FORMATTER);
            String endOfDay = today.atTime(23, 59, 59).format(DATE_FORMATTER);

            SearchResponse<Void> response = elasticsearchClient.search(s -> s
                    .index(indexName)
                    .size(0)
                    .query(q -> q
                            .bool(b -> {
                                b.must(m -> m
                                        .range(r -> r
                                                .field("collectedAt")
                                                .gte(co.elastic.clients.json.JsonData.of(startOfDay))
                                                .lte(co.elastic.clients.json.JsonData.of(endOfDay))
                                        )
                                );

                                if (source != null && !source.equals("all")) {
                                    b.must(m -> m.term(t -> t.field("source").value(source)));
                                }
                                if (type != null && !type.equals("all")) {
                                    b.must(m -> m.term(t -> t.field("type").value(type)));
                                }

                                return b;
                            })
                    )
                    .aggregations("top_keywords", a -> a
                            .terms(t -> t
                                    .field("keyword")
                                    .size(limit)
                            )
                            .aggregations("type_info", sa -> sa
                                    .terms(st -> st
                                            .field("type")
                                            .size(1)
                                    )
                            )
                    ),
                    Void.class
            );

            List<KeywordItem> results = new ArrayList<>();
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

                results.add(new KeywordItem(keyword, keywordType, count));
            }

            log.debug("Elasticsearch 조회 완료: {}개 키워드", results.size());
            return results;

        } catch (Exception e) {
            log.error("Elasticsearch 조회 실패: source={}, type={}", source, type, e);
            return List.of();
        }
    }
}
