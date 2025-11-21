package com.realtime.trend.collection.batch.youtube;

import com.realtime.trend.collection.domain.YouTubeVideo;
import com.realtime.trend.collection.service.youtube.YouTubeApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

/**
 * YouTube 동영상 ItemReader
 * YouTube API를 호출하여 인기 급상승 동영상 조회
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class YouTubeVideoReader implements ItemReader<YouTubeVideo> {

    private final YouTubeApiClient youTubeApiClient;
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

        return null; // 더 이상 읽을 아이템이 없음
    }

    /**
     * YouTube API 호출하여 동영상 목록 가져오기
     */
    private void initialize() {
        List<YouTubeVideo> videos = youTubeApiClient.fetchTrendingVideos();
        this.videoIterator = videos.iterator();
        log.info("YouTube 동영상 수집 완료: {}개", videos.size());
    }
}
