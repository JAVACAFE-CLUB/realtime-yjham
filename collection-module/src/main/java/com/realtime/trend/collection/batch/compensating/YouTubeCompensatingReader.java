package com.realtime.trend.collection.batch.compensating;

import com.realtime.trend.collection.domain.PublishStatus;
import com.realtime.trend.collection.domain.YouTubeVideo;
import com.realtime.trend.collection.repository.YouTubeVideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;

/**
 * YouTube 보상 트랜잭션 ItemReader
 * PENDING 상태인 동영상을 조회
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class YouTubeCompensatingReader implements ItemReader<YouTubeVideo> {

    private static final int RETRY_AFTER_MINUTES = 60; // 1시간 후 재시도

    private final YouTubeVideoRepository youTubeVideoRepository;
    private Iterator<YouTubeVideo> videoIterator;
    private boolean initialized = false;

    @Override
    public YouTubeVideo read() {
        if (!initialized) {
            initialize();
            initialized = true;
        }

        if (videoIterator != null && videoIterator.hasNext()) {
            return videoIterator.next();
        }

        return null;
    }

    /**
     * PENDING 상태인 동영상 조회
     */
    private void initialize() {
        LocalDateTime retryThreshold = LocalDateTime.now().minusMinutes(RETRY_AFTER_MINUTES);
        List<YouTubeVideo> pendingVideos = youTubeVideoRepository
                .findByPublishStatusAndCollectedAtBefore(PublishStatus.PENDING, retryThreshold);

        this.videoIterator = pendingVideos.iterator();
        log.info("보상 트랜잭션 대상 YouTube 동영상: {}개", pendingVideos.size());
    }
}
