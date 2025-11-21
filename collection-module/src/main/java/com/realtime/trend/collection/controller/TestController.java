package com.realtime.trend.collection.controller;

import com.realtime.trend.collection.service.crawler.HtmlCrawler;
import com.realtime.trend.collection.service.crawler.NewsArticle;
import com.realtime.trend.collection.service.crawler.RssItem;
import com.realtime.trend.collection.service.crawler.RssReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 크롤링 테스트 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final RssReader rssReader;
    private final HtmlCrawler htmlCrawler;

    /**
     * RSS 피드 테스트
     */
    @GetMapping("/rss")
    public Map<String, Object> testRss(@RequestParam String publisher) {
        Map<String, Object> response = new HashMap<>();
        try {
            String feedUrl;
            if ("경향신문".equals(publisher)) {
                feedUrl = "https://www.khan.co.kr/rss/rssdata/total_news.xml";
            } else if ("국민일보".equals(publisher)) {
                feedUrl = "https://www.kmib.co.kr/rss/data/kmibRssAll.xml";
            } else {
                response.put("success", false);
                response.put("message", "지원하지 않는 언론사입니다. (경향신문, 국민일보만 가능)");
                return response;
            }

            List<RssItem> items = rssReader.readFeed(feedUrl, publisher);

            response.put("success", true);
            response.put("count", items.size());
            response.put("items", items.subList(0, Math.min(5, items.size()))); // 처음 5개만

        } catch (Exception e) {
            log.error("RSS 테스트 실패", e);
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * HTML 크롤링 테스트
     */
    @GetMapping("/crawl")
    public Map<String, Object> testCrawl(
            @RequestParam String url,
            @RequestParam String publisher) {
        Map<String, Object> response = new HashMap<>();
        try {
            log.info("크롤링 테스트 시작 - URL: {}, Publisher: {}", url, publisher);

            NewsArticle article = htmlCrawler.crawl(url, publisher);

            if (article == null) {
                response.put("success", false);
                response.put("message", "크롤링 실패 - null 반환");
                return response;
            }

            response.put("success", true);
            response.put("article", Map.of(
                "url", article.getUrl(),
                "title", article.getTitle() != null ? article.getTitle() : "",
                "titleLength", article.getTitle() != null ? article.getTitle().length() : 0,
                "contentLength", article.getContent() != null ? article.getContent().length() : 0,
                "contentPreview", article.getContent() != null && article.getContent().length() > 100
                    ? article.getContent().substring(0, 100) + "..."
                    : article.getContent(),
                "publishedAt", article.getPublishedAt(),
                "publisher", article.getPublisher(),
                "author", article.getAuthor() != null ? article.getAuthor() : "",
                "category", article.getCategory() != null ? article.getCategory() : ""
            ));

        } catch (Exception e) {
            log.error("크롤링 테스트 실패 - URL: {}", url, e);
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("stackTrace", e.toString());
        }
        return response;
    }
}
