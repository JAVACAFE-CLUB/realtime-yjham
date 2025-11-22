package com.realtime.trend.collection.youtube.domain;

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
 * YouTube 동영상 데이터 도메인 모델
 */
@Document(collection = "youtube")
@Getter
@Builder
@ToString
public class YouTubeVideo {

    @Id
    private String id;

    /**
     * YouTube 동영상 ID (중복 체크 기준)
     */
    @Indexed(unique = true)
    private String videoId;

    /**
     * 제목
     */
    private String title;

    /**
     * 설명
     */
    private String description;

    /**
     * 채널명
     */
    private String channelTitle;

    /**
     * 발행 시각
     */
    private LocalDateTime publishedAt;

    /**
     * 카테고리 ID
     */
    private String categoryId;

    /**
     * 태그 목록
     */
    private List<String> tags;

    /**
     * 조회수
     */
    private Long viewCount;

    /**
     * 좋아요 수
     */
    private Long likeCount;

    /**
     * 댓글 수
     */
    private Long commentCount;

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
    public YouTubeVideo markAsPublished() {
        return YouTubeVideo.builder()
                .id(this.id)
                .videoId(this.videoId)
                .title(this.title)
                .description(this.description)
                .channelTitle(this.channelTitle)
                .publishedAt(this.publishedAt)
                .categoryId(this.categoryId)
                .tags(this.tags)
                .viewCount(this.viewCount)
                .likeCount(this.likeCount)
                .commentCount(this.commentCount)
                .collectedAt(this.collectedAt)
                .publishStatus(PublishStatus.PUBLISHED)
                .publishedToKafkaAt(LocalDateTime.now())
                .build();
    }
}
