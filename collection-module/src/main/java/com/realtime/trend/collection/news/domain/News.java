package com.realtime.trend.collection.news.domain;

import com.realtime.trend.collection.messaging.PublishStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 뉴스 데이터 도메인 모델
 */
@Document(collection = "news")
@Getter
@Builder
@ToString
public class News {

    @Id
    private String id;

    /**
     * 뉴스 URL (중복 체크 기준)
     */
    @Indexed(unique = true)
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
     * 언론사명
     */
    private String publisher;

    /**
     * 기자명
     */
    private String author;

    /**
     * 카테고리
     */
    private String category;

    /**
     * 태그 목록
     */
    private List<String> tags;

    /**
     * 수집 시각
     */
    @Indexed
    private LocalDateTime collectedAt;

    /**
     * Kafka 발행 상태
     */
    @Indexed
    private PublishStatus publishStatus;

    /**
     * Kafka 발행 시각
     */
    private LocalDateTime publishedToKafkaAt;

    /**
     * 발행 상태를 PUBLISHED로 변경
     */
    public News markAsPublished() {
        return News.builder()
                .id(this.id)
                .url(this.url)
                .title(this.title)
                .content(this.content)
                .publishedAt(this.publishedAt)
                .publisher(this.publisher)
                .author(this.author)
                .category(this.category)
                .tags(this.tags)
                .collectedAt(this.collectedAt)
                .publishStatus(PublishStatus.PUBLISHED)
                .publishedToKafkaAt(LocalDateTime.now())
                .build();
    }
}
