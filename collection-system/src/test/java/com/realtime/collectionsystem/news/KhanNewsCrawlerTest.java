package com.realtime.collectionsystem.news;

import com.realtime.collectionsystem.news.batch.crawler.KhanNewsCrawler;
import com.realtime.collectionsystem.news.domain.NewsArticle;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class KhanNewsCrawlerTest {

    @Test
    void 경향신문_HTML_파싱_테스트() throws IOException {
        File htmlFile = new File("reference/kyunghyang-news.html");
        Document doc = Jsoup.parse(htmlFile, "UTF-8");

        // 제목 추출 - section.art_cont h1
        String title = "";
        var artCont = doc.selectFirst("section.art_cont");
        if (artCont != null) {
            var h1 = artCont.selectFirst("h1");
            if (h1 != null) {
                title = h1.text();
            }
        }
        // fallback: .article-title
        if (title.isEmpty()) {
            var headerTitle = doc.selectFirst(".article-title");
            if (headerTitle != null) {
                title = headerTitle.text();
            }
        }

        // 본문 추출 - #articleBody p.content_text
        var paragraphs = doc.select("#articleBody p.content_text");
        if (paragraphs.isEmpty()) {
            paragraphs = doc.select(".art_body p");
        }
        StringBuilder textBuilder = new StringBuilder();
        for (var p : paragraphs) {
            String pText = p.text();
            if (!pText.isEmpty() && pText.length() > 10) {
                textBuilder.append(pText).append("\n");
            }
        }
        String text = textBuilder.toString().trim();

        // 카테고리 추출 - meta[property=article:section]
        String category = "";
        var categoryMeta = doc.selectFirst("meta[property=article:section]");
        if (categoryMeta != null) {
            category = categoryMeta.attr("content");
        }

        assertThat(title).isNotBlank();
        assertThat(text).isNotBlank();
    }

    @Test
    void 경향신문_실제_웹사이트_HTML_구조_확인() throws IOException {
        String url = "https://www.khan.co.kr/article/202510202242015";
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get();

        System.out.println("=== 제목 후보 ===");
        System.out.println("section.art_cont h1: " + (doc.selectFirst("section.art_cont h1") != null ? doc.selectFirst("section.art_cont h1").text() : "없음"));
        System.out.println(".article-title: " + (doc.selectFirst(".article-title") != null ? doc.selectFirst(".article-title").text() : "없음"));

        System.out.println("\n=== 본문 후보 ===");
        System.out.println("#articleBody p.content_text 개수: " + doc.select("#articleBody p.content_text").size());
        System.out.println(".art_body p 개수: " + doc.select(".art_body p").size());

        System.out.println("\n=== 카테고리 후보 ===");
        System.out.println("meta[property=article:section]: " + (doc.selectFirst("meta[property=article:section]") != null ? doc.selectFirst("meta[property=article:section]").attr("content") : "없음"));
        System.out.println("meta[property=og:category]: " + (doc.selectFirst("meta[property=og:category]") != null ? doc.selectFirst("meta[property=og:category]").attr("content") : "없음"));

        System.out.println("\n=== 날짜 후보 ===");
        System.out.println("meta[property=article:published_time]: " + (doc.selectFirst("meta[property=article:published_time]") != null ? doc.selectFirst("meta[property=article:published_time]").attr("content") : "없음"));
    }
}
