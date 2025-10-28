package com.realtime.cleansingsystem.news.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 뉴스 원본 데이터
 * collection-system에서 저장한 데이터 조회용
 */
@Document(collection = "news_articles")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsArticle {

    @Id
    private String id;

    @Indexed
    private String source;

    private String title;

    private String text;

    @Indexed(unique = true)
    private String url;

    private String category;

    private LocalDateTime createdDate;

    @Indexed
    private LocalDateTime collectedDate;

    @Indexed
    @Builder.Default
    private Boolean publishedToKafka = false;
}
