package com.realtime.collectionsystem.news;

import com.realtime.collectionsystem.news.batch.crawler.DongaNewsCrawler;
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

        // 제목 추출 - h2.sub_tit 사용
        String title = "";
        var titleElement = doc.selectFirst("h2.sub_tit");
        if (titleElement != null) {
            title = titleElement.text();
        }

        // 본문 추출 - section.news_view
        var newsView = doc.selectFirst("section.news_view");
        String text = "";
        if (newsView != null) {
            newsView.select("script, style, figure, .view_ad06, .view_m_adA, .view_m_adK").remove();
            text = newsView.text();
        }

        // 카테고리 추출 - meta[name=categoryname]
        String category = "";
        var categoryMeta = doc.selectFirst("meta[name=categoryname]");
        if (categoryMeta != null) {
            String categoryContent = categoryMeta.attr("content");
            if (categoryContent != null && !categoryContent.isEmpty()) {
                category = categoryContent.split(",")[0].trim();
            }
        }

        assertThat(title).isNotBlank();
        assertThat(text).isNotBlank();
    }

    @Test
    void 동아일보_실제_웹사이트_HTML_구조_확인() throws IOException {
        String url = "https://www.donga.com/news/Opinion/article/all/20251020/132600780/2";
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get();

        System.out.println("=== 제목 후보 ===");
        System.out.println("h2.sub_tit: " + (doc.selectFirst("h2.sub_tit") != null ? doc.selectFirst("h2.sub_tit").text() : "없음"));
        System.out.println("title 태그: " + (doc.selectFirst("title") != null ? doc.selectFirst("title").text() : "없음"));

        System.out.println("\n=== 본문 후보 ===");
        System.out.println("section.news_view 있음: " + (doc.selectFirst("section.news_view") != null));

        System.out.println("\n=== 카테고리 후보 ===");
        System.out.println("meta[name=categoryname]: " + (doc.selectFirst("meta[name=categoryname]") != null ? doc.selectFirst("meta[name=categoryname]").attr("content") : "없음"));

        System.out.println("\n=== 날짜 후보 ===");
        System.out.println("meta[property=dd:published_time]: " + (doc.selectFirst("meta[property=dd:published_time]") != null ? doc.selectFirst("meta[property=dd:published_time]").attr("content") : "없음"));
        System.out.println("meta[property=article:published_time]: " + (doc.selectFirst("meta[property=article:published_time]") != null ? doc.selectFirst("meta[property=article:published_time]").attr("content") : "없음"));
    }
}
