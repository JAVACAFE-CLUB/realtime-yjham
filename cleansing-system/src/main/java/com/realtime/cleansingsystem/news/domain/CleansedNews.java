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
 * 정제된 뉴스 데이터
 * cleansing database에 저장
 */
@Document(collection = "cleansed_news")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CleansedNews {

    @Id
    private String id;  // 원본 NewsArticle의 id와 동일하게 유지

    @Indexed
    private String source;

    private String title;

    private String cleanedText;  // 정제된 텍스트

    private String category;

    private LocalDateTime createdDate;

    private LocalDateTime collectedDate;

    @Indexed
    private LocalDateTime cleansedDate;

    public static CleansedNews from(NewsArticle article, String cleanedText) {
        return CleansedNews.builder()
                .id(article.getId())
                .source(article.getSource())
                .title(article.getTitle())
                .cleanedText(cleanedText)
                .category(article.getCategory())
                .createdDate(article.getCreatedDate())
                .collectedDate(article.getCollectedDate())
                .cleansedDate(LocalDateTime.now())
                .build();
    }
}
