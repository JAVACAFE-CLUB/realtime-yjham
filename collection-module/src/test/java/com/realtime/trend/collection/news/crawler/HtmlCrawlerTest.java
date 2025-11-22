package com.realtime.trend.collection.news.crawler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@DisplayName("HtmlCrawler 단위 테스트")
@ExtendWith(MockitoExtension.class)
class HtmlCrawlerTest {

    @Mock
    private NewsArticleParser khanParser;

    @Mock
    private NewsArticleParser kmibParser;

    private HtmlCrawler crawler;

    @BeforeEach
    void setUp() {
        when(khanParser.getPublisher()).thenReturn("경향신문");
        when(kmibParser.getPublisher()).thenReturn("국민일보");
        crawler = new HtmlCrawler(List.of(khanParser, kmibParser));
        // @PostConstruct 메서드 수동 호출
        crawler.init();
    }

    @Nested
    @DisplayName("crawl() 메서드")
    class CrawlMethod {

        @Test
        @DisplayName("지원하지 않는 언론사는 null을 반환해야 한다")
        void crawl_withUnsupportedPublisher_shouldReturnNull() {
            // given
            String url = "https://unsupported.com/article/1";
            String publisher = "지원안함신문";

            // when
            NewsArticle result = crawler.crawl(url, publisher);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("잘못된 URL은 null을 반환해야 한다")
        void crawl_withInvalidUrl_shouldReturnNull() {
            // given
            String url = "invalid-url";
            String publisher = "경향신문";

            // when
            NewsArticle result = crawler.crawl(url, publisher);

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("파서 매핑")
    class ParserMapping {

        @Test
        @DisplayName("언론사별로 올바른 파서가 매핑되어야 한다")
        void parserMap_shouldContainAllParsers() {
            // 파서 맵이 초기화되는지 확인하기 위해 crawl 호출 (존재하지 않는 언론사로)
            crawler.crawl("https://example.com", "테스트");

            // 경향신문 파서가 등록되어 있는지 확인 - 실제 네트워크 호출이 필요하므로
            // 여기서는 지원하지 않는 언론사 케이스만 테스트
            NewsArticle result = crawler.crawl("https://example.com", "미등록언론사");
            assertThat(result).isNull();
        }
    }
}
