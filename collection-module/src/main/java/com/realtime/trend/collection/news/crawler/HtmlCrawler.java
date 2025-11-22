package com.realtime.trend.collection.news.crawler;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTML 크롤러
 * 전략 패턴으로 언론사별 파서를 관리
 * Rate Limiting 적용으로 대상 서버 부하 방지
 */
@Slf4j
@Component
public class HtmlCrawler {

    private final List<NewsArticleParser> parsers;
    private Map<String, NewsArticleParser> parserMap;
    private RateLimiter rateLimiter;

    /**
     * 초당 허용 요청 수
     */
    @Value("${collection.news.crawler.requests-per-second:2}")
    private int requestsPerSecond;

    /**
     * Rate Limiter 대기 타임아웃 (밀리초)
     */
    @Value("${collection.news.crawler.timeout-ms:10000}")
    private int timeoutMs;

    public HtmlCrawler(List<NewsArticleParser> parsers) {
        this.parsers = parsers;
    }

    @PostConstruct
    public void init() {
        // Resilience4j RateLimiter 설정
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(requestsPerSecond)
                .timeoutDuration(Duration.ofMillis(timeoutMs))
                .build();

        RateLimiterRegistry registry = RateLimiterRegistry.of(config);
        this.rateLimiter = registry.rateLimiter("htmlCrawler");
        log.info("RateLimiter 초기화 완료: 초당 {}회 요청 허용", requestsPerSecond);
    }

    /**
     * 언론사별 파서 맵 초기화
     */
    private void initializeParserMap() {
        if (parserMap == null) {
            parserMap = new HashMap<>();
            for (NewsArticleParser parser : parsers) {
                parserMap.put(parser.getPublisher(), parser);
            }
            log.info("파서 초기화 완료: {}개", parserMap.size());
        }
    }

    /**
     * URL에서 기사 크롤링
     *
     * @param url       기사 URL
     * @param publisher 언론사명
     * @return 크롤링된 기사 정보
     */
    public NewsArticle crawl(String url, String publisher) {
        initializeParserMap();

        NewsArticleParser parser = parserMap.get(publisher);
        if (parser == null) {
            log.error("지원하지 않는 언론사: {}", publisher);
            return null;
        }

        try {
            // Rate Limiting 적용
            if (!acquireRateLimitPermission()) {
                log.warn("Rate limit 초과로 요청 스킵: {}", url);
                return null;
            }

            // HTML 가져오기
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(timeoutMs)
                    .get();

            // 파싱
            NewsArticle article = parser.parse(doc, url);

            if (article != null) {
                log.debug("크롤링 성공: {} - {}", publisher, url);
            } else {
                log.warn("파싱 실패: {} - {}", publisher, url);
            }

            return article;

        } catch (Exception e) {
            log.error("크롤링 실패: {} - {}", publisher, url, e);
            return null;
        }
    }

    /**
     * Rate Limiting 적용 (Resilience4j RateLimiter 사용)
     * 설정된 초당 요청 수를 초과하면 대기
     *
     * @return true if permission acquired, false otherwise
     */
    private boolean acquireRateLimitPermission() {
        try {
            return rateLimiter.acquirePermission();
        } catch (Exception e) {
            log.warn("RateLimiter permission 획득 실패", e);
            return false;
        }
    }
}
