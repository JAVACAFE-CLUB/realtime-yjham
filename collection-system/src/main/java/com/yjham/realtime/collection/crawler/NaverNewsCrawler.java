package com.yjham.realtime.collection.crawler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class NaverNewsCrawler {

    private static final String NAVER_NEWS_BASE_URL = "https://news.naver.com";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";
    private static final String ACCEPT_HEADER = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8";
    private static final String ACCEPT_LANGUAGE_HEADER = "ko-KR,ko;q=0.8,en-US;q=0.5,en;q=0.3";
    private static final int TIMEOUT_MILLIS = 15000;
    private static final int CRAWL_DELAY_MILLIS = 1000;

    private final KafkaMessagePublisher kafkaMessagePublisher;

    // 중복 크롤링 방지인데 어떻게 관리할까...
    private final Set<String> visitedUrls = new HashSet<>();

    @Scheduled(fixedRate = 3600000)
    public void crawlNaverNews() {
        try {
            log.info("네이버 뉴스 크롤링 시작");
            crawlMainNewsPage();
            log.info("네이버 뉴스 크롤링 완료");
        } catch (Exception e) {
            log.error("네이버 뉴스 크롤링 실패", e);
        }
    }

    private void crawlMainNewsPage() throws IOException {
        Document doc = createConnection(NAVER_NEWS_BASE_URL).get();
        Elements newsLinks = doc.select("a[href*='/main/read.naver'], a[href*='/article/']");
        
        log.info("메인 페이지에서 {}개의 뉴스 링크를 발견했습니다", newsLinks.size());
        
        for (Element link : newsLinks) {
            String newsUrl = link.attr("abs:href");
            if (isValidNewsUrl(newsUrl)) {
                crawlNewsArticle(newsUrl);
                visitedUrls.add(newsUrl);
                waitBetweenCrawls();
            }
        }
    }

    private boolean isValidNewsUrl(String url) {
        if (url == null || !url.contains("news.naver.com")) {
            return false;
        }
        
        return !visitedUrls.contains(url);
    }

    private void waitBetweenCrawls() {
        try {
            Thread.sleep(CRAWL_DELAY_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("크롤링 대기 중 인터럽트가 발생했습니다");
        }
    }

    private void crawlNewsArticle(String url) {
        try {
            Document doc = createConnection(url).get();
            
            String title = extractTitle(doc);
            String content = extractContent(doc);
            String publishTime = extractPublishTime(doc);

            if (isValidArticle(title, content)) {
                NewsData newsData = new NewsData(title, content, url, publishTime, LocalDateTime.now());
                kafkaMessagePublisher.publishNewsData(newsData);
                log.info("뉴스 크롤링 성공: 제목 : {}, URL : {}", title, url);
            } else {
                log.warn("빈 콘텐츠로 인해 스킵된 URL: {}", url);
            }

        } catch (Exception e) {
            log.error("뉴스 크롤링 실패: {} - {}", url, e.getMessage());
        }
    }

    // JSoup 커넥션 생성
    private Connection createConnection(String url) {
        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .header("Accept", ACCEPT_HEADER)
                .header("Accept-Language", ACCEPT_LANGUAGE_HEADER)
                .timeout(TIMEOUT_MILLIS);
    }

    // 제목, 내용, 발행일 추출
    private String extractTitle(Document doc) {
        String title = doc.select("#title_area .media_end_head_headline").text();

        if (title.isEmpty()) {
            title = doc.select(".media_end_head_headline").text();
        }

        if (title.isEmpty()) {
            title = doc.select("#title_area span").text();
        }

        return title;
    }

    private String extractContent(Document doc) {
        return doc.select("#dic_area, .go_trans._article_content").text();
    }

    private String extractPublishTime(Document doc) {
        String publishTime = doc.select(".media_end_head_info_datestamp_time, ._ARTICLE_DATE_TIME")
                                .attr("data-date-time");

        if (publishTime.isEmpty()) {
            publishTime = doc.select(".media_end_head_info_datestamp_time, ._ARTICLE_DATE_TIME").text();
        }

        return publishTime;
    }

    // 기사 유효성 검사
    private boolean isValidArticle(String title, String content) {
        return title != null && !title.trim().isEmpty() && 
               content != null && !content.trim().isEmpty();
    }
}