package com.realtime.trend.collection.news.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("KhanParser 단위 테스트")
class KhanParserTest {

    private KhanParser parser;

    @BeforeEach
    void setUp() {
        parser = new KhanParser();
    }

    @Test
    @DisplayName("getPublisher()는 '경향신문'을 반환해야 한다")
    void getPublisher_shouldReturnKhan() {
        assertThat(parser.getPublisher()).isEqualTo("경향신문");
    }

    @Nested
    @DisplayName("parse() 메서드")
    class ParseMethod {

        @Test
        @DisplayName("정상적인 HTML에서 모든 필드를 파싱해야 한다")
        void parse_withValidHtml_shouldExtractAllFields() {
            // given
            String html = createValidKhanHtml();
            Document doc = Jsoup.parse(html);
            String url = "https://www.khan.co.kr/article/12345";

            // when
            NewsArticle article = parser.parse(doc, url);

            // then
            assertThat(article).isNotNull();
            assertThat(article.getUrl()).isEqualTo(url);
            assertThat(article.getTitle()).isEqualTo("테스트 기사 제목");
            assertThat(article.getContent()).contains("첫 번째 문단 내용");
            assertThat(article.getContent()).contains("두 번째 문단 내용");
            assertThat(article.getAuthor()).isEqualTo("홍길동 기자");
            assertThat(article.getCategory()).isEqualTo("정치");
            assertThat(article.getPublisher()).isEqualTo("경향신문");
        }

        @Test
        @DisplayName("발행 시각을 ISO 형식에서 파싱해야 한다")
        void parse_withValidDateTime_shouldParsePublishedAt() {
            // given
            String html = createValidKhanHtml();
            Document doc = Jsoup.parse(html);

            // when
            NewsArticle article = parser.parse(doc, "https://example.com");

            // then
            assertThat(article.getPublishedAt()).isNotNull();
            assertThat(article.getPublishedAt().getYear()).isEqualTo(2025);
            assertThat(article.getPublishedAt().getMonthValue()).isEqualTo(11);
            assertThat(article.getPublishedAt().getDayOfMonth()).isEqualTo(20);
        }

        @Test
        @DisplayName("제목이 없는 경우 빈 문자열을 반환해야 한다")
        void parse_withoutTitle_shouldReturnEmptyString() {
            // given
            String html = "<html><body></body></html>";
            Document doc = Jsoup.parse(html);

            // when
            NewsArticle article = parser.parse(doc, "https://example.com");

            // then
            assertThat(article).isNotNull();
            assertThat(article.getTitle()).isEmpty();
        }

        @Test
        @DisplayName("본문이 없는 경우 빈 문자열을 반환해야 한다")
        void parse_withoutContent_shouldReturnEmptyString() {
            // given
            String html = """
                <html>
                <body>
                    <p class="article-title">제목만 있는 기사</p>
                </body>
                </html>
                """;
            Document doc = Jsoup.parse(html);

            // when
            NewsArticle article = parser.parse(doc, "https://example.com");

            // then
            assertThat(article).isNotNull();
            assertThat(article.getContent()).isEmpty();
        }

        @Test
        @DisplayName("메타 태그가 없는 경우 기본값을 반환해야 한다")
        void parse_withoutMetaTags_shouldReturnDefaults() {
            // given
            String html = """
                <html>
                <body>
                    <p class="article-title">메타 태그 없는 기사</p>
                    <p class="content_text">본문 내용</p>
                </body>
                </html>
                """;
            Document doc = Jsoup.parse(html);

            // when
            NewsArticle article = parser.parse(doc, "https://example.com");

            // then
            assertThat(article).isNotNull();
            assertThat(article.getAuthor()).isEmpty();
            assertThat(article.getCategory()).isEmpty();
            // 발행 시각이 없으면 현재 시각으로 설정되므로 null이 아님
            assertThat(article.getPublishedAt()).isNotNull();
        }

        @Test
        @DisplayName("여러 문단의 본문을 올바르게 합쳐야 한다")
        void parse_withMultipleParagraphs_shouldConcatenateContent() {
            // given
            String html = """
                <html>
                <body>
                    <p class="content_text">첫 번째 문단</p>
                    <p class="content_text">두 번째 문단</p>
                    <p class="content_text">세 번째 문단</p>
                </body>
                </html>
                """;
            Document doc = Jsoup.parse(html);

            // when
            NewsArticle article = parser.parse(doc, "https://example.com");

            // then
            assertThat(article.getContent())
                .contains("첫 번째 문단")
                .contains("두 번째 문단")
                .contains("세 번째 문단");
        }
    }

    private String createValidKhanHtml() {
        return """
            <html>
            <head>
                <meta property="article:published_time" content="2025-11-20T22:48:00+09:00">
                <meta property="article:author" content="홍길동 기자">
                <meta property="article:section" content="정치">
            </head>
            <body>
                <p class="article-title">테스트 기사 제목</p>
                <p class="content_text">첫 번째 문단 내용입니다.</p>
                <p class="content_text">두 번째 문단 내용입니다.</p>
            </body>
            </html>
            """;
    }
}
