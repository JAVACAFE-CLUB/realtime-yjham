package com.realtime.trend.collection.youtube.batch;

import com.realtime.trend.collection.messaging.DataPublisher;
import com.realtime.trend.collection.youtube.domain.YouTubeVideo;
import com.realtime.trend.collection.youtube.repository.YouTubeVideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * YouTube 보상 트랜잭션 ItemWriter
 * PENDING 상태인 동영상을 Kafka로 재발행 시도
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class YouTubeCompensatingWriter implements ItemWriter<YouTubeVideo> {

    private final YouTubeVideoRepository youTubeVideoRepository;
    private final DataPublisher dataPublisher;

    @Override
    public void write(Chunk<? extends YouTubeVideo> chunk) {
        for (YouTubeVideo video : chunk) {
            try {
                // Kafka 재발행 시도
                dataPublisher.publishYouTubeVideo(video);

                // 발행 성공 시 PUBLISHED 상태로 변경
                YouTubeVideo publishedVideo = video.markAsPublished();
                youTubeVideoRepository.save(publishedVideo);
                log.info("보상 트랜잭션 성공: {}", video.getVideoId());

            } catch (Exception e) {
                // 발행 실패 시 PENDING 상태 유지 (다음 실행에서 재시도)
                log.warn("보상 트랜잭션 실패 (재시도 예정): {}", video.getVideoId(), e);
            }
        }
    }
}
