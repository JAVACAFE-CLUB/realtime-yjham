package com.realtime.trend.indexing.service;

import com.realtime.trend.indexing.document.KeywordDocument;
import com.realtime.trend.indexing.dto.ProcessedMessage;
import com.realtime.trend.indexing.repository.KeywordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class KeywordIndexingService {

    private static final Logger log = LoggerFactory.getLogger(KeywordIndexingService.class);

    private final KeywordRepository keywordRepository;

    @Value("${elasticsearch.retention-days}")
    private int retentionDays;

    public KeywordIndexingService(KeywordRepository keywordRepository) {
        this.keywordRepository = keywordRepository;
    }

    public void indexKeywords(ProcessedMessage message) {
        if (message.keywords() == null || message.keywords().isEmpty()) {
            log.debug("색인할 키워드 없음: {}", message.sourceId());
            return;
        }

        List<KeywordDocument> documents = message.keywords().stream()
                .map(entity -> new KeywordDocument(
                        entity.keyword(),
                        entity.type(),
                        message.source(),
                        message.sourceId(),
                        message.collectedAt(),
                        message.title()
                ))
                .toList();

        keywordRepository.saveAll(documents);
        log.info("키워드 색인 완료: {} - {}개", message.sourceId(), documents.size());
    }

    public void deleteOldKeywords() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);
        long deletedCount = keywordRepository.deleteByCollectedAtBefore(threshold);
        log.info("오래된 키워드 삭제: {}개 ({}일 이전)", deletedCount, retentionDays);
    }
}
