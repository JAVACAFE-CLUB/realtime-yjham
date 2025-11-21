package com.realtime.trend.collection.batch.youtube;

import com.realtime.trend.collection.domain.YouTubeVideo;
import com.realtime.trend.collection.repository.YouTubeVideoRepository;
import com.realtime.trend.collection.service.kafka.DataPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * YouTube 동영상 ItemWriter
 * MongoDB 저장 및 Kafka 발행 (Best Effort + Compensating Transaction 패턴)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class YouTubeVideoWriter implements ItemWriter<YouTubeVideo> {

    private final YouTubeVideoRepository youTubeVideoRepository;
    private final DataPublisher dataPublisher;

    @Override
    public void write(Chunk<? extends YouTubeVideo> chunk) {
        for (YouTubeVideo video : chunk) {
            try {
                // 1. MongoDB 저장 (PENDING 상태)
                YouTubeVideo savedVideo = youTubeVideoRepository.save(video);
                log.debug("YouTube 동영상 저장 완료: {}", savedVideo.getVideoId());

                // 2. Kafka 발행 시도
                try {
                    dataPublisher.publishYouTubeVideo(savedVideo);

                    // 3. 발행 성공 시 PUBLISHED 상태로 변경
                    YouTubeVideo publishedVideo = savedVideo.markAsPublished();
                    youTubeVideoRepository.save(publishedVideo);
                    log.debug("YouTube 동영상 발행 완료: {}", publishedVideo.getVideoId());

                } catch (Exception e) {
                    // Kafka 발행 실패 시 PENDING 상태 유지 (보상 트랜잭션에서 재시도)
                    log.warn("Kafka 발행 실패 (PENDING 상태 유지): {}", savedVideo.getVideoId(), e);
                }

            } catch (Exception e) {
                log.error("YouTube 동영상 저장 실패: {}", video.getVideoId(), e);
                // MongoDB 저장 실패는 해당 아이템만 스킵하고 계속 진행
            }
        }
    }
}
