package com.realtime.collectionsystem.youtube.batch;

import com.realtime.collectionsystem.youtube.domain.YoutubeVideo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class YoutubeVideoReader implements ItemReader<YoutubeVideo> {

    private final YoutubeApiClient apiClient;
    private Iterator<YoutubeVideo> videos;

    @Override
    public YoutubeVideo read() {
        if (videos == null) {
            List<YoutubeVideo> videoList = apiClient.fetchMostPopularVideos();
            videos = videoList.iterator();
        }

        if (videos.hasNext()) {
            return videos.next();
        }

        return null;
    }
}
