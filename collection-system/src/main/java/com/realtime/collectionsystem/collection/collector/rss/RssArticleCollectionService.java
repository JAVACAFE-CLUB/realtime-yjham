package com.realtime.collectionsystem.collection.collector.rss;

import com.realtime.collectionsystem.collection.collector.rss.strategy.ArticleCrawler;
import com.realtime.collectionsystem.collection.collector.rss.strategy.ArticleCrawlerFactory;
import com.realtime.collectionsystem.domain.Article;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RssArticleCollectionService {

    private final RssLinkExtractor rssLinkExtractor;
    private final ArticleCrawlerFactory crawlerFactory;
    private final Executor executor = Executors.newFixedThreadPool(5);

    public List<Article> collectArticles() {
        log.info("기사 수집 시작");

        List<String> articleLinks = rssLinkExtractor.extractArticleLinks();
        log.info("RSS에서 {}개 링크 추출 완료", articleLinks.size());

        List<CompletableFuture<Article>> futures = articleLinks.stream()
                .map(this::crawlArticleAsync)
                .toList();

        List<Article> articles = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();

        log.info("기사 수집 완료: {}개 기사 수집", articles.size());
        return articles;
    }

    private CompletableFuture<Article> crawlArticleAsync(String url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ArticleCrawler crawler = crawlerFactory.getCrawler(url);
                return crawler.crawlArticle(url);
            } catch (Exception e) {
                log.error("기사 크롤링 실패: {}", url, e);
                return null;
            }
        }, executor);
    }

    public List<String> getSupportedSources() {
        return crawlerFactory.getSupportedSources();
    }
}