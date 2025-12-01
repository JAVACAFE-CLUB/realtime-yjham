package com.realtime.trend.collection.youtube;

import com.realtime.trend.collection.core.domain.PublishStatus;
import com.realtime.trend.collection.core.source.DataSource;
import com.realtime.trend.collection.youtube.batch.YouTubeVideoProcessor;
import com.realtime.trend.collection.youtube.batch.YouTubeVideoReader;
import com.realtime.trend.collection.youtube.domain.YouTubeVideo;
import com.realtime.trend.collection.youtube.repository.YouTubeVideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * YouTube 데이터 소스 구현체
 */
@Component
@RequiredArgsConstructor
public class YouTubeDataSource implements DataSource<YouTubeVideo> {

    private final YouTubeVideoRepository youTubeVideoRepository;
    private final YouTubeVideoReader youTubeVideoReader;
    private final YouTubeVideoProcessor youTubeVideoProcessor;

    @Value("${collection.youtube.enabled:true}")
    private boolean enabled;

    @Value("${collection.youtube.cron:0 */15 * * * *}")
    private String collectionCron;

    @Value("${collection.youtube.compensating-cron:0 */30 * * * *}")
    private String compensatingCron;

    @Value("${collection.youtube.topic:raw-youtube}")
    private String topicName;

    @Override
    public String getSourceName() {
        return "youtube";
    }

    @Override
    public String getTopicName() {
        return topicName;
    }

    @Override
    public String getCollectionCron() {
        return collectionCron;
    }

    @Override
    public String getCompensatingCron() {
        return compensatingCron;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public ItemReader<YouTubeVideo> createCollectionReader() {
        youTubeVideoReader.reset();  // 상태 초기화
        return youTubeVideoReader;
    }

    @Override
    public ItemProcessor<YouTubeVideo, YouTubeVideo> createProcessor() {
        return youTubeVideoProcessor;
    }

    @Override
    public List<YouTubeVideo> findPendingItems() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        return youTubeVideoRepository.findByPublishStatusAndCollectedAtBefore(
                PublishStatus.PENDING, threshold);
    }

    @Override
    public YouTubeVideo save(YouTubeVideo entity) {
        return youTubeVideoRepository.save(entity);
    }
}
