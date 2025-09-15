package com.realtime.collectionsystem.collection.collector.rss.strategy;

import com.realtime.collectionsystem.domain.Article;
import com.realtime.collectionsystem.domain.NewsSource;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Slf4j
@Component
public class KyunghyangCrawler implements ArticleCrawler {

    private static final NewsSource NEWS_SOURCE = NewsSource.KHAN;

    @Override
    public Article crawlArticle(String url) {
        try {
            log.debug("[CRAWLER] 경향신문 기사 크롤링: {}", url);

            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(10000)
                    .get();

            String title = extractTitle(doc);
            String content = extractContent(doc);
            String author = extractAuthor(doc);
            LocalDateTime publishedDate = extractPublishedDate(doc);

            return Article.builder()
                    .url(url)
                    .title(title)
                    .content(content)
                    .author(author)
                    .source(NEWS_SOURCE.getDisplayName())
                    .publishedDate(publishedDate)
                    .collectedDate(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("경향신문 기사 크롤링 실패: {}", url, e);
            throw new RuntimeException("기사 크롤링 중 오류 발생", e);
        }
    }

    @Override
    public boolean canHandle(String url) {
        return NEWS_SOURCE.canHandle(url);
    }

    @Override
    public String getSourceName() {
        return NEWS_SOURCE.getDisplayName();
    }

    private String extractTitle(Document doc) {
        Element titleElement = doc.selectFirst("article header h1");
        return titleElement != null ? titleElement.text().trim() : "";
    }

    private String extractContent(Document doc) {
        Elements contentElements = doc.select("#articleBody .content_text");
        return contentElements.stream()
                .map(Element::text)
                .collect(Collectors.joining("\n\n"));
    }

    private String extractAuthor(Document doc) {
        Element authorElement = doc.selectFirst(".editor a");
        if (authorElement != null) {
            String authorText = authorElement.text().trim();
            // "안광호 기자" 형태에서 "기자" 제거
            return authorText.replace(" 기자", "");
        }
        return null;
    }

    private LocalDateTime extractPublishedDate(Document doc) {
        try {
            Element dateElement = doc.selectFirst(".date p");
            if (dateElement != null) {
                String dateText = dateElement.text().trim();
                // "입력 2025.09.15 08:38" 형태에서 날짜 부분 추출
                String dateOnly = dateText.replace("입력 ", "");
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
                return LocalDateTime.parse(dateOnly, formatter);
            }
        } catch (Exception e) {
            log.warn("경향신문 날짜 파싱 실패", e);
        }
        return null;
    }
}