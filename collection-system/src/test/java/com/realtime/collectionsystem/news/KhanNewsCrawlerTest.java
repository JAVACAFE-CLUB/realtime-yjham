package com.realtime.collectionsystem.news;

import com.realtime.collectionsystem.news.batch.KhanNewsCrawler;
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

        String title = doc.selectFirst(".headline") != null
                ? doc.selectFirst(".headline").text()
                : "";

        assertThat(title).isNotBlank();
    }

    @Test
    void 경향신문_실제_웹사이트_HTML_구조_확인() throws IOException {
        String url = "https://www.khan.co.kr/article/202510202242015";
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get();

        System.out.println("=== 제목 후보 ===");
        System.out.println(".headline: " + (doc.selectFirst(".headline") != null ? doc.selectFirst(".headline").text() : "없음"));
        System.out.println("h1.headline: " + (doc.selectFirst("h1.headline") != null ? doc.selectFirst("h1.headline").text() : "없음"));
        System.out.println(".art_header h1: " + (doc.selectFirst(".art_header h1") != null ? doc.selectFirst(".art_header h1").text() : "없음"));
        
        System.out.println("\n=== 본문 후보 ===");
        System.out.println("#articleBody 개수: " + doc.select("#articleBody p").size());
        System.out.println(".art_body 개수: " + doc.select(".art_body p").size());
        System.out.println("#articeBody 개수: " + doc.select("#articeBody p").size());
        
        System.out.println("\n=== 카테고리 후보 ===");
        System.out.println(".category: " + (doc.selectFirst(".category") != null ? doc.selectFirst(".category").text() : "없음"));
        System.out.println(".sec_menu: " + (doc.selectFirst(".sec_menu") != null ? doc.selectFirst(".sec_menu").text() : "없음"));
    }
}
