package com.realtime.trend.indexing.repository;

import com.realtime.trend.indexing.document.KeywordDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface KeywordRepository extends ElasticsearchRepository<KeywordDocument, String> {

    long deleteByCollectedAtBefore(LocalDateTime dateTime);
}
