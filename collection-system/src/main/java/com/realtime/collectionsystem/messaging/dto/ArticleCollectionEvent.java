package com.realtime.collectionsystem.messaging.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ArticleCollectionEvent {
    private final String articleId;         // MinIO 경로 (2025/09/15/경향신문/12345678.json)
    private final String url;
    private final String title;
    private final String source;
    private final String author;
    private final LocalDateTime publishedDate;
    private final LocalDateTime collectedDate;
    private final int contentLength;        // 본문 길이
    private final String eventType;         // ARTICLE_COLLECTED
    private final String version;           // 메시지 버전
}