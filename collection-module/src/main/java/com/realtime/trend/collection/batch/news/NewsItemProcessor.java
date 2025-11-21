package com.realtime.trend.collection.batch.news;

import com.realtime.trend.collection.domain.News;
import com.realtime.trend.collection.repository.NewsRepository;
import com.realtime.trend.collection.service.crawler.HtmlCrawler;
import com.realtime.trend.collection.service.crawler.NewsArticle;
import com.realtime.trend.collection.service.crawler.RssItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 뉴스 ItemProcessor
 * RSS 아이템을 HTML 크롤링하여 뉴스 도메인 객체로 변환
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewsItemProcessor implements ItemProcessor<RssItem, News> {

    private final NewsRepository newsRepository;
    private final HtmlCrawler htmlCrawler;

    @Override
    public News process(RssItem item) {
        // 중복 체크
        if (newsRepository.existsByUrl(item.getUrl())) {
            log.debug("이미 수집된 기사: {}", item.getUrl());
            return null; // Skip
        }

        // HTML 크롤링
        NewsArticle article = htmlCrawler.crawl(item.getUrl(), item.getPublisher());
        if (article == null) {
            log.warn("크롤링 실패한 기사 스킵: {}", item.getUrl());
            return null; // Skip
        }

        // News 도메인 객체 생성
        return News.builder()
                .url(article.getUrl())
                .title(article.getTitle())
                .content(article.getContent())
                .publishedAt(article.getPublishedAt())
                .publisher(article.getPublisher())
                .author(article.getAuthor())
                .category(article.getCategory())
                .collectedAt(LocalDateTime.now())
                .build();
    }
}
