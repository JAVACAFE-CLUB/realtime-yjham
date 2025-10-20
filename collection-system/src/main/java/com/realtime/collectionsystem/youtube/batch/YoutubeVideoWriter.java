package com.realtime.collectionsystem.youtube.batch;

import com.realtime.collectionsystem.youtube.domain.YoutubeVideo;
import com.realtime.collectionsystem.youtube.repository.YoutubeVideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class YoutubeVideoWriter implements ItemWriter<YoutubeVideo> {

    private final YoutubeVideoRepository repository;

    @Override
    public void write(Chunk<? extends YoutubeVideo> chunk) {
        repository.saveAll(chunk.getItems());
        log.info("YouTube 동영상 {} 건 저장 완료", chunk.size());
    }
}
