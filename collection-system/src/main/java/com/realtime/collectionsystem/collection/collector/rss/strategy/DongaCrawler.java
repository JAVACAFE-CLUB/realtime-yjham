package com.realtime.collectionsystem.collection.collector.rss.strategy;

import com.realtime.collectionsystem.domain.Article;
import com.realtime.collectionsystem.domain.NewsSource;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DongaCrawler implements ArticleCrawler {

    private static final NewsSource NEWS_SOURCE = NewsSource.DONGA;

    @Override
    public Article crawlArticle(String url) {
        try {
            log.debug("[CRAWLER] 동아일보 기사 크롤링: {}", url);

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
            log.error("동아일보 기사 크롤링 실패: {}", url, e);
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
        Element titleElement = doc.selectFirst(".head_group h1");
        return titleElement != null ? titleElement.text().trim() : "";
    }

    private String extractContent(Document doc) {
        Element contentElement = doc.selectFirst(".news_view");
        if (contentElement == null) {
            return "";
        }

        // 이미지 요소 제거
        contentElement.select("figure").remove();

        // 텍스트 노드들을 수집하여 문단 구분
        return contentElement.childNodes().stream()
                .filter(node -> node instanceof TextNode)
                .map(Node::toString)
                .map(text -> text.replace("<br>", "\n"))
                .map(String::trim)
                .filter(text -> !text.isEmpty())
                .collect(Collectors.joining("\n\n"));
    }

    private String extractAuthor(Document doc) {
        // 먼저 .byline에서 기자명 추출 시도
        Element authorElement = doc.selectFirst(".byline");
        if (authorElement != null) {
            String authorText = authorElement.text().trim();
            // "남혜정 기자 namduck2@donga.com" 형태에서 기자명만 추출
            if (authorText.contains("기자")) {
                return authorText.split("기자")[0].trim();
            }
            return authorText;
        }

        // .byline이 없으면 .news_info strong에서 대체
        Element sourceElement = doc.selectFirst(".news_info strong");
        if (sourceElement != null) {
            return sourceElement.text().trim();
        }

        return null;
    }


    private LocalDateTime extractPublishedDate(Document doc) {
        try {
            Element dateElement = doc.selectFirst(".news_info button span[aria-hidden='true']");
            if (dateElement != null) {
                String dateText = dateElement.text().trim();
                // "2025-09-15 08:33" 형태
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                return LocalDateTime.parse(dateText, formatter);
            }
        } catch (Exception e) {
            log.warn("동아일보 날짜 파싱 실패", e);
        }
        return null;
    }
}