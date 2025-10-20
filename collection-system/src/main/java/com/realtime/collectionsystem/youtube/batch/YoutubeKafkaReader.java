package com.realtime.collectionsystem.youtube.batch;

import com.realtime.collectionsystem.youtube.domain.YoutubeVideo;
import com.realtime.collectionsystem.youtube.repository.YoutubeVideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class YoutubeKafkaReader implements ItemReader<YoutubeVideo> {

    private final YoutubeVideoRepository repository;
    private Iterator<YoutubeVideo> videos;

    @Override
    public YoutubeVideo read() {
        if (videos == null) {
            List<YoutubeVideo> unpublished = repository.findByPublishedToKafkaFalse();
            videos = unpublished.iterator();
        }

        if (videos.hasNext()) {
            return videos.next();
        }

        return null;
    }
}
