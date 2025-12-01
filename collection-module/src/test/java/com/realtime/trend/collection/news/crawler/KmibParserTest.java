package com.realtime.trend.collection.news.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("KmibParser 단위 테스트")
class KmibParserTest {

    private KmibParser parser;

    @BeforeEach
    void setUp() {
        parser = new KmibParser();
    }

    @Test
    @DisplayName("getPublisher()는 '국민일보'를 반환해야 한다")
    void getPublisher_shouldReturnKmib() {
        assertThat(parser.getPublisher()).isEqualTo("국민일보");
    }

    @Nested
    @DisplayName("parse() 메서드")
    class ParseMethod {

        @Test
        @DisplayName("정상적인 HTML을 파싱하여 NewsArticle을 반환해야 한다")
        void parse_withValidHtml_shouldReturnNewsArticle() {
            // given
            String html = """
                <html>
                <head>
                    <meta property="article:published_time" content="2025-11-21T10:30:00+09:00">
                    <meta property="dable:author" content="홍길동 기자">
                    <meta property="article:section" content="정치">
                </head>
                <body>
                    <h1 class="article_headline">테스트 뉴스 제목입니다</h1>
                    <div id="articleBody">
                        첫 번째 문단입니다.<br>
                        두 번째 문단입니다.
                    </div>
                </body>
                </html>
                """;
            Document doc = Jsoup.parse(html);
            String url = "https://kmib.co.kr/article/1234";

            // when
            NewsArticle article = parser.parse(doc, url);

            // then
            assertThat(article).isNotNull();
            assertThat(article.getUrl()).isEqualTo(url);
            assertThat(article.getTitle()).isEqualTo("테스트 뉴스 제목입니다");
            assertThat(article.getContent()).contains("첫 번째 문단");
            assertThat(article.getAuthor()).isEqualTo("홍길동 기자");
            assertThat(article.getCategory()).isEqualTo("정치");
            assertThat(article.getPublisher()).isEqualTo("국민일보");
        }

        @Test
        @DisplayName("제목이 없는 HTML도 처리해야 한다")
        void parse_withMissingTitle_shouldReturnArticleWithEmptyTitle() {
            // given
            String html = """
                <html>
                <head>
                    <meta property="article:published_time" content="2025-11-21T10:30:00+09:00">
                </head>
                <body>
                    <div id="articleBody">본문 내용</div>
                </body>
                </html>
                """;
            Document doc = Jsoup.parse(html);

            // when
            NewsArticle article = parser.parse(doc, "https://kmib.co.kr/article/1");

            // then
            assertThat(article).isNotNull();
            assertThat(article.getTitle()).isEmpty();
        }

        @Test
        @DisplayName("본문이 없는 HTML도 처리해야 한다")
        void parse_withMissingContent_shouldReturnArticleWithEmptyContent() {
            // given
            String html = """
                <html>
                <head>
                    <meta property="article:published_time" content="2025-11-21T10:30:00+09:00">
                </head>
                <body>
                    <h1 class="article_headline">제목만 있는 기사</h1>
                </body>
                </html>
                """;
            Document doc = Jsoup.parse(html);

            // when
            NewsArticle article = parser.parse(doc, "https://kmib.co.kr/article/2");

            // then
            assertThat(article).isNotNull();
            assertThat(article.getContent()).isEmpty();
        }

        @Test
        @DisplayName("발행 시각이 없으면 현재 시각을 사용해야 한다")
        void parse_withMissingPublishedAt_shouldUseCurrentTime() {
            // given
            String html = """
                <html>
                <body>
                    <h1 class="article_headline">제목</h1>
                    <div id="articleBody">본문</div>
                </body>
                </html>
                """;
            Document doc = Jsoup.parse(html);
            LocalDateTime before = LocalDateTime.now();

            // when
            NewsArticle article = parser.parse(doc, "https://kmib.co.kr/article/3");

            // then
            assertThat(article.getPublishedAt())
                    .isAfterOrEqualTo(before)
                    .isBeforeOrEqualTo(LocalDateTime.now());
        }

        @Test
        @DisplayName("기자명이 없으면 빈 문자열을 반환해야 한다")
        void parse_withMissingAuthor_shouldReturnEmptyAuthor() {
            // given
            String html = """
                <html>
                <head>
                    <meta property="article:published_time" content="2025-11-21T10:30:00+09:00">
                </head>
                <body>
                    <h1 class="article_headline">제목</h1>
                </body>
                </html>
                """;
            Document doc = Jsoup.parse(html);

            // when
            NewsArticle article = parser.parse(doc, "https://kmib.co.kr/article/4");

            // then
            assertThat(article.getAuthor()).isEmpty();
        }

        @Test
        @DisplayName("카테고리가 없으면 빈 문자열을 반환해야 한다")
        void parse_withMissingCategory_shouldReturnEmptyCategory() {
            // given
            String html = """
                <html>
                <head>
                    <meta property="article:published_time" content="2025-11-21T10:30:00+09:00">
                </head>
                <body>
                    <h1 class="article_headline">제목</h1>
                </body>
                </html>
                """;
            Document doc = Jsoup.parse(html);

            // when
            NewsArticle article = parser.parse(doc, "https://kmib.co.kr/article/5");

            // then
            assertThat(article.getCategory()).isEmpty();
        }

        @Test
        @DisplayName("본문에 br 태그가 있으면 줄바꿈으로 변환해야 한다")
        void parse_withBrTags_shouldConvertToNewlines() {
            // given
            String html = """
                <html>
                <body>
                    <h1 class="article_headline">제목</h1>
                    <div id="articleBody">
                        첫 번째 줄<br>두 번째 줄<br>세 번째 줄
                    </div>
                </body>
                </html>
                """;
            Document doc = Jsoup.parse(html);

            // when
            NewsArticle article = parser.parse(doc, "https://kmib.co.kr/article/6");

            // then
            assertThat(article.getContent()).contains("\n");
        }

        @Test
        @DisplayName("잘못된 발행 시각 형식은 현재 시각으로 대체해야 한다")
        void parse_withInvalidDateFormat_shouldUseCurrentTime() {
            // given
            String html = """
                <html>
                <head>
                    <meta property="article:published_time" content="invalid-date">
                </head>
                <body>
                    <h1 class="article_headline">제목</h1>
                </body>
                </html>
                """;
            Document doc = Jsoup.parse(html);
            LocalDateTime before = LocalDateTime.now();

            // when
            NewsArticle article = parser.parse(doc, "https://kmib.co.kr/article/7");

            // then
            assertThat(article.getPublishedAt())
                    .isAfterOrEqualTo(before)
                    .isBeforeOrEqualTo(LocalDateTime.now());
        }
    }
}
