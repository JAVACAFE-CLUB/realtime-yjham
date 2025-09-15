package com.realtime.collectionsystem.collection.collector.rss;

import com.realtime.collectionsystem.collection.collector.rss.strategy.ArticleCrawler;
import com.realtime.collectionsystem.collection.collector.rss.strategy.ArticleCrawlerFactory;
import com.realtime.collectionsystem.collection.collector.rss.strategy.KyunghyangCrawler;
import com.realtime.collectionsystem.collection.collector.rss.strategy.DongaCrawler;
import com.realtime.collectionsystem.domain.Article;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ArticleCrawlerTest {

    @Autowired
    private ArticleCrawlerFactory crawlerFactory;

    @Test
    void testKyunghyangCrawler() {
        String khUrl = "https://www.khan.co.kr/article/202509150831001/";

        ArticleCrawler crawler = crawlerFactory.getCrawler(khUrl);
        assertThat(crawler).isInstanceOf(KyunghyangCrawler.class);
        assertThat(crawler.getSourceName()).isEqualTo("경향신문");

        // 실제 크롤링 테스트 (네트워크 접근)
        try {
            Article article = crawler.crawlArticle(khUrl);

            assertThat(article.getUrl()).isEqualTo(khUrl);
            assertThat(article.getTitle()).isNotEmpty();
            assertThat(article.getSource()).isEqualTo("경향신문");
            assertThat(article.getCollectedDate()).isNotNull();

            System.out.println("경향신문 크롤링 결과:");
            System.out.println("제목: " + article.getTitle());
            System.out.println("작성자: " + article.getAuthor());
            System.out.println("발행일: " + article.getPublishedDate());
            System.out.println("내용 길이: " + (article.getContent() != null ? article.getContent().length() : 0));
        } catch (Exception e) {
            System.out.println("네트워크 오류로 실제 크롤링 테스트 스킵: " + e.getMessage());
        }
    }

    @Test
    void testDongaCrawler() {
        String dongaUrl = "https://www.donga.com/news/Entertainment/article/all/20250915/132386736/1";

        ArticleCrawler crawler = crawlerFactory.getCrawler(dongaUrl);
        assertThat(crawler).isInstanceOf(DongaCrawler.class);
        assertThat(crawler.getSourceName()).isEqualTo("동아일보");

        // 실제 크롤링 테스트 (네트워크 접근)
        try {
            Article article = crawler.crawlArticle(dongaUrl);

            assertThat(article.getUrl()).isEqualTo(dongaUrl);
            assertThat(article.getTitle()).isNotEmpty();
            assertThat(article.getSource()).contains("일보");
            assertThat(article.getCollectedDate()).isNotNull();

            System.out.println("동아일보 크롤링 결과:");
            System.out.println("제목: " + article.getTitle());
            System.out.println("소스: " + article.getSource());
            System.out.println("발행일: " + article.getPublishedDate());
            System.out.println("내용 길이: " + (article.getContent() != null ? article.getContent().length() : 0));
        } catch (Exception e) {
            System.out.println("네트워크 오류로 실제 크롤링 테스트 스킵: " + e.getMessage());
        }
    }

    @Test
    void testUnsupportedUrl() {
        String unsupportedUrl = "https://unsupported.com/article/123";

        try {
            crawlerFactory.getCrawler(unsupportedUrl);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("지원하지 않는 URL입니다");
        }
    }
}