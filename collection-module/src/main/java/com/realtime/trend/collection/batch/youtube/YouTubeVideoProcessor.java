package com.realtime.trend.collection.batch.youtube;

import com.realtime.trend.collection.domain.YouTubeVideo;
import com.realtime.trend.collection.repository.YouTubeVideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * YouTube 동영상 ItemProcessor
 * 중복 체크 및 데이터 검증
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class YouTubeVideoProcessor implements ItemProcessor<YouTubeVideo, YouTubeVideo> {

    private final YouTubeVideoRepository youTubeVideoRepository;

    @Override
    public YouTubeVideo process(YouTubeVideo video) {
        // 중복 체크
        if (youTubeVideoRepository.existsByVideoId(video.getVideoId())) {
            log.debug("이미 수집된 동영상: {}", video.getVideoId());
            return null; // Skip
        }

        return video;
    }
}
