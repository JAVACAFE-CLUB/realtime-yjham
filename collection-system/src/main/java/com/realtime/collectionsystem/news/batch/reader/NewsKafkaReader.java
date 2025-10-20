package com.realtime.collectionsystem.news.batch.reader;

import com.realtime.collectionsystem.news.domain.NewsArticle;
import com.realtime.collectionsystem.news.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NewsKafkaReader implements ItemReader<NewsArticle> {

    private final NewsArticleRepository repository;
    private Iterator<NewsArticle> articles;

    @Override
    public NewsArticle read() {
        if (articles == null) {
            List<NewsArticle> unpublished = repository.findByPublishedToKafkaFalse();
            articles = unpublished.iterator();
        }

        if (articles.hasNext()) {
            return articles.next();
        }

        return null;
    }
}
