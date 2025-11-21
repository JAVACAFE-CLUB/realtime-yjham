package com.realtime.trend.collection.service.crawler;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RSS 아이템 DTO
 */
@Getter
@Builder
@ToString
public class RssItem {

    /**
     * 기사 URL
     */
    private String url;

    /**
     * 제목
     */
    private String title;

    /**
     * 요약/설명
     */
    private String description;

    /**
     * 발행 시각
     */
    private LocalDateTime publishedAt;

    /**
     * 카테고리
     */
    private String category;

    /**
     * 언론사명
     */
    private String publisher;
}
