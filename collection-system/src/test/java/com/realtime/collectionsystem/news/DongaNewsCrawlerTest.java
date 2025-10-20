package com.realtime.collectionsystem.news;

import com.realtime.collectionsystem.news.batch.DongaNewsCrawler;
import com.realtime.collectionsystem.news.domain.NewsArticle;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class DongaNewsCrawlerTest {

    @Test
    void 동아일보_HTML_파싱_테스트() throws IOException {
        File htmlFile = new File("reference/donga-news.html");
        Document doc = Jsoup.parse(htmlFile, "UTF-8");

        String title = doc.selectFirst(".article_title h1") != null
                ? doc.selectFirst(".article_title h1").text()
                : "";

        assertThat(title).isNotBlank();
    }

    @Test
    void 동아일보_실제_웹사이트_HTML_구조_확인() throws IOException {
        String url = "https://www.donga.com/news/Opinion/article/all/20251020/132600780/2";
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get();

        System.out.println("=== 제목 후보 ===");
        System.out.println(".article_title h1: " + (doc.selectFirst(".article_title h1") != null ? doc.selectFirst(".article_title h1").text() : "없음"));
        System.out.println("h1: " + (doc.selectFirst("h1") != null ? doc.selectFirst("h1").text() : "없음"));
        System.out.println(".title: " + (doc.selectFirst(".title") != null ? doc.selectFirst(".title").text() : "없음"));
        
        System.out.println("\n=== 본문 후보 ===");
        System.out.println(".article_txt p 개수: " + doc.select(".article_txt p").size());
        System.out.println(".article_body p 개수: " + doc.select(".article_body p").size());
        System.out.println("#article_body p 개수: " + doc.select("#article_body p").size());
        
        System.out.println("\n=== 카테고리 후보 ===");
        System.out.println(".category: " + (doc.selectFirst(".category") != null ? doc.selectFirst(".category").text() : "없음"));
        System.out.println(".location: " + (doc.selectFirst(".location") != null ? doc.selectFirst(".location").text() : "없음"));
    }
}
