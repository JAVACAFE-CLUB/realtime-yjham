package com.realtime.collectionsystem.messaging.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class WikiPageCollectionEvent {
    private final String pageId;                // MinIO 경로 기반 고유 ID
    private final Long wikiPageId;              // 위키피디아 페이지 ID
    private final String title;
    private final Integer namespace;
    private final String namespaceName;
    private final Long revisionId;
    private final LocalDateTime lastModified;
    private final String contributor;
    private final LocalDateTime collectedDate;
    private final int contentLength;            // 본문 길이
    private final List<String> categories;      // 카테고리 목록
    private final int internalLinksCount;       // 내부 링크 개수
    private final int externalLinksCount;       // 외부 링크 개수
    private final String eventType;             // WIKIPAGE_COLLECTED
    private final String version;               // 메시지 버전
}