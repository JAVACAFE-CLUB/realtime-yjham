package com.realtime.trend.collection.service.crawler;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 경향신문 파서
 */
@Slf4j
@Component
public class KhanParser implements NewsArticleParser {

    private static final String PUBLISHER = "경향신문";
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Override
    public NewsArticle parse(Document doc, String url) {
        try {
            return NewsArticle.builder()
                    .url(url)
                    .title(parseTitle(doc))
                    .content(parseContent(doc))
                    .publishedAt(parsePublishedAt(doc))
                    .author(parseAuthor(doc))
                    .category(parseCategory(doc))
                    .publisher(PUBLISHER)
                    .build();
        } catch (Exception e) {
            log.error("경향신문 파싱 실패: {}", url, e);
            return null;
        }
    }

    @Override
    public String getPublisher() {
        return PUBLISHER;
    }

    /**
     * 제목 파싱: <p class="article-title">
     */
    private String parseTitle(Document doc) {
        Element titleElement = doc.selectFirst("p.article-title");
        if (titleElement != null) {
            return titleElement.text().trim();
        }
        return "";
    }

    /**
     * 본문 파싱: <p class="content_text"> 여러 개
     */
    private String parseContent(Document doc) {
        Elements contentElements = doc.select("p.content_text");
        StringBuilder content = new StringBuilder();

        for (Element element : contentElements) {
            String text = element.text().trim();
            if (!text.isEmpty()) {
                content.append(text).append("\n\n");
            }
        }

        return content.toString().trim();
    }

    /**
     * 발행 시각 파싱: <meta property="article:published_time" content="2025-11-20T22:48:00+09:00">
     */
    private LocalDateTime parsePublishedAt(Document doc) {
        Element metaElement = doc.selectFirst("meta[property=article:published_time]");
        if (metaElement != null) {
            String content = metaElement.attr("content");
            try {
                return LocalDateTime.parse(content, DATE_FORMATTER);
            } catch (Exception e) {
                log.warn("발행 시각 파싱 실패: {}", content, e);
            }
        }
        return LocalDateTime.now();
    }

    /**
     * 기자명 파싱: <meta property="article:author" content="김기범 기자">
     */
    private String parseAuthor(Document doc) {
        Element metaElement = doc.selectFirst("meta[property=article:author]");
        if (metaElement != null) {
            return metaElement.attr("content").trim();
        }
        return "";
    }

    /**
     * 카테고리 파싱: <meta property="article:section" content="카테고리">
     */
    private String parseCategory(Document doc) {
        Element metaElement = doc.selectFirst("meta[property=article:section]");
        if (metaElement != null) {
            return metaElement.attr("content").trim();
        }
        return "";
    }
}
