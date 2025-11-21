package com.realtime.trend.collection.service.crawler;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 크롤링된 뉴스 기사 DTO
 */
@Getter
@Builder
@ToString
public class NewsArticle {

    /**
     * 기사 URL
     */
    private String url;

    /**
     * 제목
     */
    private String title;

    /**
     * 본문
     */
    private String content;

    /**
     * 발행 시각
     */
    private LocalDateTime publishedAt;

    /**
     * 기자명
     */
    private String author;

    /**
     * 카테고리
     */
    private String category;

    /**
     * 언론사명
     */
    private String publisher;
}
