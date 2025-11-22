package com.realtime.trend.collection.messaging;

import com.realtime.trend.collection.news.domain.News;
import com.realtime.trend.collection.youtube.domain.YouTubeVideo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka Producer
 * 수집된 데이터를 Kafka 토픽으로 발행
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataPublisher {

    private static final String NEWS_TOPIC = "raw-news";
    private static final String YOUTUBE_TOPIC = "raw-youtube";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 뉴스 데이터 발행
     *
     * @param news 뉴스 데이터
     */
    public void publishNews(News news) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(NEWS_TOPIC, news.getUrl(), news);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("뉴스 발행 성공: {} - offset: {}",
                        news.getUrl(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("뉴스 발행 실패: {}", news.getUrl(), ex);
            }
        });
    }

    /**
     * YouTube 동영상 데이터 발행
     *
     * @param video YouTube 동영상 데이터
     */
    public void publishYouTubeVideo(YouTubeVideo video) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(YOUTUBE_TOPIC, video.getVideoId(), video);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("YouTube 동영상 발행 성공: {} - offset: {}",
                        video.getVideoId(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("YouTube 동영상 발행 실패: {}", video.getVideoId(), ex);
            }
        });
    }
}
