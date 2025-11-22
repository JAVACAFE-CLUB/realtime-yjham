package com.realtime.trend.collection.news.domain;

import com.realtime.trend.collection.core.domain.Publishable;
import com.realtime.trend.collection.core.domain.PublishStatus;
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
public class News implements Publishable {

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
     * 재시도 횟수
     */
    @Builder.Default
    private int retryCount = 0;

    @Override
    public String getIdentifier() {
        return this.url;
    }

    @Override
    public News markAsPublished() {
        return copyBuilder()
                .publishStatus(PublishStatus.PUBLISHED)
                .publishedToKafkaAt(LocalDateTime.now())
                .build();
    }

    @Override
    public int getRetryCount() {
        return this.retryCount;
    }

    @Override
    public News incrementRetryCount() {
        return copyBuilder()
                .retryCount(this.retryCount + 1)
                .build();
    }

    @Override
    public News markAsFailed() {
        return copyBuilder()
                .publishStatus(PublishStatus.FAILED)
                .build();
    }

    private NewsBuilder copyBuilder() {
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
                .publishStatus(this.publishStatus)
                .publishedToKafkaAt(this.publishedToKafkaAt)
                .retryCount(this.retryCount);
    }
}
