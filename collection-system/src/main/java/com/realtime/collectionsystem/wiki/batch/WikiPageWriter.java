package com.realtime.collectionsystem.wiki.batch;

import com.realtime.collectionsystem.wiki.domain.WikiPage;
import com.realtime.collectionsystem.wiki.repository.WikiPageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WikiPageWriter implements ItemWriter<WikiPage> {

    private final WikiPageRepository repository;

    @Override
    public void write(Chunk<? extends WikiPage> chunk) {
        repository.saveAll(chunk.getItems());
        log.info("위키피디아 {} 건 저장 완료", chunk.size());
    }
}
