package com.realtime.trend.collection.repository;

import com.realtime.trend.collection.domain.PublishStatus;
import com.realtime.trend.collection.domain.YouTubeVideo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * YouTube 동영상 데이터 Repository
 */
@Repository
public interface YouTubeVideoRepository extends MongoRepository<YouTubeVideo, String> {

    /**
     * videoId로 동영상 조회 (중복 체크용)
     */
    Optional<YouTubeVideo> findByVideoId(String videoId);

    /**
     * videoId 존재 여부 확인
     */
    boolean existsByVideoId(String videoId);

    /**
     * 발행 상태로 조회
     */
    List<YouTubeVideo> findByPublishStatus(PublishStatus publishStatus);

    /**
     * 발행 상태이고 특정 시간 이전에 수집된 데이터 조회 (보상 트랜잭션용)
     */
    List<YouTubeVideo> findByPublishStatusAndCollectedAtBefore(
            PublishStatus publishStatus,
            LocalDateTime collectedAt
    );

    /**
     * 특정 기간의 동영상 조회
     */
    List<YouTubeVideo> findByCollectedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 채널별 동영상 조회
     */
    List<YouTubeVideo> findByChannelTitle(String channelTitle);
}
