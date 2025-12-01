package com.realtime.trend.collection.news.batch;

import com.realtime.trend.collection.news.crawler.HtmlCrawler;
import com.realtime.trend.collection.news.crawler.NewsArticle;
import com.realtime.trend.collection.news.crawler.RssItem;
import com.realtime.trend.collection.news.domain.News;
import com.realtime.trend.collection.news.repository.NewsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("NewsItemProcessor 단위 테스트")
@ExtendWith(MockitoExtension.class)
class NewsItemProcessorTest {

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private HtmlCrawler htmlCrawler;

    @InjectMocks
    private NewsItemProcessor processor;

    private RssItem testRssItem;
    private NewsArticle testArticle;

    @BeforeEach
    void setUp() {
        testRssItem = RssItem.builder()
                .url("https://example.com/news/1")
                .title("테스트 뉴스 제목")
                .description("테스트 설명")
                .publishedAt(LocalDateTime.now())
                .category("경제")
                .publisher("경향신문")
                .build();

        testArticle = NewsArticle.builder()
                .url("https://example.com/news/1")
                .title("테스트 뉴스 제목")
                .content("테스트 본문 내용입니다.")
                .publishedAt(LocalDateTime.now())
                .author("홍길동 기자")
                .category("경제")
                .publisher("경향신문")
                .build();
    }

    @Nested
    @DisplayName("process() 메서드")
    class ProcessMethod {

        @Test
        @DisplayName("정상적인 RSS 아이템을 News 객체로 변환해야 한다")
        void process_withValidRssItem_shouldReturnNews() throws Exception {
            // given
            when(newsRepository.existsByUrl(anyString())).thenReturn(false);
            when(htmlCrawler.crawl(anyString(), anyString())).thenReturn(testArticle);

            // when
            News result = processor.process(testRssItem);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getUrl()).isEqualTo(testArticle.getUrl());
            assertThat(result.getTitle()).isEqualTo(testArticle.getTitle());
            assertThat(result.getContent()).isEqualTo(testArticle.getContent());
            assertThat(result.getPublisher()).isEqualTo(testArticle.getPublisher());
            assertThat(result.getAuthor()).isEqualTo(testArticle.getAuthor());
            assertThat(result.getCollectedAt()).isNotNull();

            verify(newsRepository).existsByUrl(testRssItem.getUrl());
            verify(htmlCrawler).crawl(testRssItem.getUrl(), testRssItem.getPublisher());
        }

        @Test
        @DisplayName("이미 수집된 기사는 null을 반환해야 한다 (중복 체크)")
        void process_withDuplicateUrl_shouldReturnNull() throws Exception {
            // given
            when(newsRepository.existsByUrl(anyString())).thenReturn(true);

            // when
            News result = processor.process(testRssItem);

            // then
            assertThat(result).isNull();
            verify(newsRepository).existsByUrl(testRssItem.getUrl());
            verifyNoInteractions(htmlCrawler);
        }

        @Test
        @DisplayName("크롤링 실패 시 null을 반환해야 한다")
        void process_whenCrawlFails_shouldReturnNull() throws Exception {
            // given
            when(newsRepository.existsByUrl(anyString())).thenReturn(false);
            when(htmlCrawler.crawl(anyString(), anyString())).thenReturn(null);

            // when
            News result = processor.process(testRssItem);

            // then
            assertThat(result).isNull();
            verify(htmlCrawler).crawl(testRssItem.getUrl(), testRssItem.getPublisher());
        }

        @Test
        @DisplayName("수집 시각(collectedAt)이 현재 시각으로 설정되어야 한다")
        void process_shouldSetCollectedAtToCurrentTime() throws Exception {
            // given
            when(newsRepository.existsByUrl(anyString())).thenReturn(false);
            when(htmlCrawler.crawl(anyString(), anyString())).thenReturn(testArticle);
            LocalDateTime beforeProcess = LocalDateTime.now();

            // when
            News result = processor.process(testRssItem);

            // then
            assertThat(result.getCollectedAt())
                .isAfterOrEqualTo(beforeProcess)
                .isBeforeOrEqualTo(LocalDateTime.now());
        }
    }
}
