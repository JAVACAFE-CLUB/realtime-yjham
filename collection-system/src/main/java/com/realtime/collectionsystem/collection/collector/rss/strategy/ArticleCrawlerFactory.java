package com.realtime.collectionsystem.collection.collector.rss.strategy;

import com.realtime.collectionsystem.domain.NewsSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleCrawlerFactory {

    private final List<ArticleCrawler> crawlers;

    public ArticleCrawler getCrawler(String url) {
        return crawlers.stream()
                .filter(crawler -> crawler.canHandle(url))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("해당 URL을 처리할 수 있는 크롤러를 찾을 수 없습니다: {}", url);
                    return new IllegalArgumentException("지원하지 않는 URL입니다: " + url);
                });
    }

    public List<String> getSupportedSources() {
        return Arrays.stream(NewsSource.values())
                .map(NewsSource::getDisplayName)
                .toList();
    }

    public List<NewsSource> getSupportedNewsSource() {
        return Arrays.asList(NewsSource.values());
    }
}