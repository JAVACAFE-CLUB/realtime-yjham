package com.realtime.trend.collection.service.crawler;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 국민일보 파서
 */
@Slf4j
@Component
public class KmibParser implements NewsArticleParser {

    private static final String PUBLISHER = "국민일보";
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
            log.error("국민일보 파싱 실패: {}", url, e);
            return null;
        }
    }

    @Override
    public String getPublisher() {
        return PUBLISHER;
    }

    /**
     * 제목 파싱: <h1 class="article_headline">
     */
    private String parseTitle(Document doc) {
        Element titleElement = doc.selectFirst("h1.article_headline");
        if (titleElement != null) {
            return titleElement.text().trim();
        }
        return "";
    }

    /**
     * 본문 파싱: <div id="articleBody">
     * 텍스트와 <br> 태그가 섞여 있음
     */
    private String parseContent(Document doc) {
        Element articleBody = doc.selectFirst("div#articleBody");
        if (articleBody == null) {
            return "";
        }

        StringBuilder content = new StringBuilder();
        for (Node node : articleBody.childNodes()) {
            if (node instanceof TextNode) {
                String text = ((TextNode) node).text().trim();
                if (!text.isEmpty()) {
                    content.append(text).append(" ");
                }
            } else if (node instanceof Element) {
                Element element = (Element) node;
                if (element.tagName().equals("br")) {
                    content.append("\n");
                }
            }
        }

        return content.toString().trim();
    }

    /**
     * 발행 시각 파싱: <meta property="article:published_time" content="2025-11-21T00:28:00+09:00">
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
     * 기자명 파싱: <meta property="dable:author" content="이름">
     */
    private String parseAuthor(Document doc) {
        Element metaElement = doc.selectFirst("meta[property=dable:author]");
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
