package com.realtime.trend.collection.news.repository;

import com.realtime.trend.collection.messaging.PublishStatus;
import com.realtime.trend.collection.news.domain.News;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 뉴스 데이터 Repository
 */
@Repository
public interface NewsRepository extends MongoRepository<News, String> {

    /**
     * URL로 뉴스 조회 (중복 체크용)
     */
    Optional<News> findByUrl(String url);

    /**
     * URL 존재 여부 확인
     */
    boolean existsByUrl(String url);

    /**
     * 발행 상태로 조회
     */
    List<News> findByPublishStatus(PublishStatus publishStatus);

    /**
     * 발행 상태이고 특정 시간 이전에 수집된 데이터 조회 (보상 트랜잭션용)
     */
    List<News> findByPublishStatusAndCollectedAtBefore(
            PublishStatus publishStatus,
            LocalDateTime collectedAt
    );

    /**
     * 특정 기간의 뉴스 조회
     */
    List<News> findByCollectedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 언론사별 뉴스 조회
     */
    List<News> findByPublisher(String publisher);
}
