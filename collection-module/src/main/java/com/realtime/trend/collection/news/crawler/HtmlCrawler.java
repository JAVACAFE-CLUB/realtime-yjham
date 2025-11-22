package com.realtime.trend.collection.news.crawler;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

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

    /**
     * 요청 간 최소 딜레이 (밀리초)
     */
    @Value("${collection.news.crawler.delay-ms:500}")
    private long delayMs;

    /**
     * 연결 타임아웃 (밀리초)
     */
    @Value("${collection.news.crawler.timeout-ms:10000}")
    private int timeoutMs;

    /**
     * 마지막 요청 시각
     */
    private final AtomicLong lastRequestTime = new AtomicLong(0);

    public HtmlCrawler(List<NewsArticleParser> parsers) {
        this.parsers = parsers;
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
            applyRateLimit();

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
     * Rate Limiting 적용
     * 마지막 요청 이후 지정된 딜레이가 지나지 않았으면 대기
     */
    private void applyRateLimit() {
        if (delayMs <= 0) {
            return;
        }

        long now = System.currentTimeMillis();
        long lastTime = lastRequestTime.get();
        long elapsed = now - lastTime;

        if (elapsed < delayMs) {
            try {
                long sleepTime = delayMs - elapsed;
                log.trace("Rate limiting: {}ms 대기", sleepTime);
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        lastRequestTime.set(System.currentTimeMillis());
    }
}
