package com.realtime.collectionsystem.youtube.batch;

import com.realtime.collectionsystem.youtube.domain.YoutubeVideo;
import com.realtime.collectionsystem.youtube.repository.YoutubeVideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class YoutubeVideoProcessor implements ItemProcessor<YoutubeVideo, YoutubeVideo> {

    private final YoutubeVideoRepository repository;

    @Override
    public YoutubeVideo process(YoutubeVideo item) {
        if (repository.existsByVideoId(item.getVideoId())) {
            log.debug("이미 수집된 동영상: {}", item.getVideoId());
            return null;
        }

        return item;
    }
}
