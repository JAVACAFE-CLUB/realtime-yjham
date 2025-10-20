package com.realtime.collectionsystem.news.batch.processor;

import com.realtime.collectionsystem.news.batch.crawler.NewsCrawler;
import com.realtime.collectionsystem.news.batch.crawler.NewsCrawlerFactory;
import com.realtime.collectionsystem.news.batch.dto.RssFeedItem;
import com.realtime.collectionsystem.news.domain.NewsArticle;
import com.realtime.collectionsystem.news.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsArticleProcessor implements ItemProcessor<RssFeedItem, NewsArticle> {

    private final NewsCrawlerFactory crawlerFactory;
    private final NewsArticleRepository repository;

    @Override
    public NewsArticle process(RssFeedItem item) {
        if (repository.existsByUrl(item.getUrl())) {
            log.debug("이미 수집된 뉴스: {}", item.getUrl());
            return null;
        }

        try {
            NewsCrawler crawler = crawlerFactory.getCrawler(item.getSource());
            return crawler.crawl(item.getUrl());
        } catch (Exception e) {
            log.error("뉴스 크롤링 실패: {}", item.getUrl(), e);
            return null;
        }
    }
}
