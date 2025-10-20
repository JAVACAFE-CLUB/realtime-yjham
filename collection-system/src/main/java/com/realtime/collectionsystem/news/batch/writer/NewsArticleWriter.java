package com.realtime.collectionsystem.news.batch.writer;

import com.realtime.collectionsystem.news.domain.NewsArticle;
import com.realtime.collectionsystem.news.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsArticleWriter implements ItemWriter<NewsArticle> {

    private final NewsArticleRepository repository;

    @Override
    public void write(Chunk<? extends NewsArticle> chunk) {
        repository.saveAll(chunk.getItems());
        log.info("뉴스 {} 건 저장 완료", chunk.size());
    }
}
