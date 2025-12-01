package com.realtime.trend.serving.service;

import com.realtime.trend.serving.dto.KeywordResponse;
import com.realtime.trend.serving.dto.KeywordResponse.KeywordItem;
import com.realtime.trend.serving.dto.KeywordResponse.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class KeywordService {

    private static final Logger log = LoggerFactory.getLogger(KeywordService.class);

    private final ElasticsearchKeywordService elasticsearchService;

    public KeywordService(ElasticsearchKeywordService elasticsearchService) {
        this.elasticsearchService = elasticsearchService;
    }

    @Cacheable(
            cacheNames = "today",
            key = "'source:' + #source + ':type:' + #type + ':category:' + #category + ':limit:' + #limit",
            unless = "#result.keywords().isEmpty()"
    )
    public KeywordResponse getKeywords(String source, String type, String category, int limit) {
        log.debug("키워드 조회 (캐시 미스): source={}, type={}, category={}, limit={}", source, type, category, limit);

        List<KeywordItem> keywords = elasticsearchService.getKeywords(source, type, category, limit);

        return new KeywordResponse(
                keywords,
                new Metadata(
                        keywords.size(),
                        limit,
                        source,
                        type,
                        category,
                        LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                )
        );
    }
}
