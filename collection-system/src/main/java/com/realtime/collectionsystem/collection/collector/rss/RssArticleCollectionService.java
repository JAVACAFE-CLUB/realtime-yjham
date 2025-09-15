package com.realtime.collectionsystem.collection.collector.rss;

import com.realtime.collectionsystem.collection.collector.rss.strategy.ArticleCrawler;
import com.realtime.collectionsystem.collection.collector.rss.strategy.ArticleCrawlerFactory;
import com.realtime.collectionsystem.domain.Article;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RssArticleCollectionService {

    private final RssLinkExtractor rssLinkExtractor;
    private final ArticleCrawlerFactory crawlerFactory;
    private final Executor executor = Executors.newFixedThreadPool(5);

    public List<Article> collectArticles() {
        long startTime = System.currentTimeMillis();
        log.info("[COLLECTION] RSS 기사 수집 시작");

        List<String> articleLinks = rssLinkExtractor.extractArticleLinks();

        if (articleLinks.isEmpty()) {
            log.warn("[COLLECTION] RSS에서 추출된 링크가 없습니다");
            return List.of();
        }

        log.info("[COLLECTION] RSS 링크 추출 완료 - 총 {}개", articleLinks.size());

        List<CompletableFuture<Article>> futures = articleLinks.stream()
                .map(this::crawlArticleAsync)
                .toList();

        List<Article> articles = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();

        // 소스별 통계 생성
        Map<String, Long> sourceStats = articles.stream()
                .collect(Collectors.groupingBy(Article::getSource, Collectors.counting()));

        String statsMessage = sourceStats.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue() + "개")
                .collect(Collectors.joining(", "));

        long duration = System.currentTimeMillis() - startTime;
        int failCount = articleLinks.size() - articles.size();

        if (failCount == 0) {
            log.info("[COLLECTION] 기사 수집 완료 - {} (총 {}개, 소요시간: {}ms)",
                    statsMessage, articles.size(), duration);
        } else {
            log.warn("[COLLECTION] 기사 수집 완료 - {} (성공: {}개, 실패: {}개, 소요시간: {}ms)",
                    statsMessage, articles.size(), failCount, duration);
        }

        return articles;
    }

    private CompletableFuture<Article> crawlArticleAsync(String url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ArticleCrawler crawler = crawlerFactory.getCrawler(url);
                return crawler.crawlArticle(url);
            } catch (Exception e) {
                log.debug("[COLLECTION] 기사 크롤링 실패 - URL: {}, 오류: {}", url, e.getMessage());
                return null;
            }
        }, executor);
    }

    public List<String> getSupportedSources() {
        return crawlerFactory.getSupportedSources();
    }
}