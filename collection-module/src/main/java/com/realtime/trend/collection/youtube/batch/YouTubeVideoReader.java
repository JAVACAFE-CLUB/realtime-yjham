package com.realtime.trend.collection.youtube.batch;

import com.realtime.trend.collection.youtube.client.YouTubeApiClient;
import com.realtime.trend.collection.youtube.domain.YouTubeVideo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

/**
 * YouTube 동영상 ItemReader
 * YouTube API를 호출하여 인기 급상승 동영상 조회
 * StepScope 적용으로 매 Step 실행마다 새 인스턴스 생성
 */
@Slf4j
@Component
public class YouTubeVideoReader implements ItemReader<YouTubeVideo> {

    private final YouTubeApiClient youTubeApiClient;
    private Iterator<YouTubeVideo> videoIterator;
    private boolean initialized = false;

    public YouTubeVideoReader(YouTubeApiClient youTubeApiClient) {
        this.youTubeApiClient = youTubeApiClient;
    }

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

    private void initialize() {
        List<YouTubeVideo> videos = youTubeApiClient.fetchTrendingVideos();
        this.videoIterator = videos.iterator();
        log.info("YouTube 동영상 수집 완료: {}개", videos.size());
    }

    /**
     * 상태 초기화 (StepScope 재사용 대비)
     */
    public void reset() {
        this.initialized = false;
        this.videoIterator = null;
    }
}
