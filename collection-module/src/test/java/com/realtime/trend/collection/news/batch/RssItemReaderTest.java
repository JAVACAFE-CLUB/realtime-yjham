package com.realtime.trend.collection.news.batch;

import com.realtime.trend.collection.news.crawler.RssItem;
import com.realtime.trend.collection.news.crawler.RssReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ExecutionContext;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("RssItemReader 단위 테스트")
@ExtendWith(MockitoExtension.class)
class RssItemReaderTest {

    @Mock
    private RssReader rssReader;

    @Mock
    private RssFeedProperties properties;

    private RssItemReader reader;

    @BeforeEach
    void setUp() {
        Map<String, String> feeds = new LinkedHashMap<>();
        feeds.put("경향신문", "https://khan.co.kr/rss");
        feeds.put("국민일보", "https://kmib.co.kr/rss");
        when(properties.getFeeds()).thenReturn(feeds);

        reader = new RssItemReader(rssReader, properties);
    }

    @Nested
    @DisplayName("ItemStream 라이프사이클")
    class ItemStreamLifecycle {

        @Test
        @DisplayName("open() 호출 시 모든 피드를 읽어야 한다")
        void open_shouldReadAllFeeds() {
            // given
            when(rssReader.readFeed(anyString(), anyString())).thenReturn(List.of());
            ExecutionContext context = new ExecutionContext();

            // when
            reader.open(context);

            // then
            verify(rssReader).readFeed("https://khan.co.kr/rss", "경향신문");
            verify(rssReader).readFeed("https://kmib.co.kr/rss", "국민일보");
        }

        @Test
        @DisplayName("close() 호출 후 read()는 null을 반환해야 한다")
        void close_shouldResetState() {
            // given
            RssItem item = createTestRssItem("https://example.com/1");
            when(rssReader.readFeed(anyString(), anyString()))
                    .thenReturn(List.of(item));
            
            reader.open(new ExecutionContext());
            assertThat(reader.read()).isNotNull();

            // when
            reader.close();

            // then
            assertThat(reader.read()).isNull();
        }

        @Test
        @DisplayName("reset() 후 open() 재호출 시 다시 피드를 읽어야 한다")
        void reset_thenOpen_shouldReadFeedsAgain() {
            // given
            when(rssReader.readFeed(anyString(), anyString())).thenReturn(List.of());
            
            reader.open(new ExecutionContext());
            reader.close();

            // when
            reader.open(new ExecutionContext());

            // then
            verify(rssReader, times(4)).readFeed(anyString(), anyString()); // 2번 * 2 피드
        }
    }

    @Nested
    @DisplayName("read() 메서드")
    class ReadMethod {

        @Test
        @DisplayName("모든 피드의 아이템을 순차적으로 반환해야 한다")
        void read_shouldReturnAllItemsSequentially() {
            // given
            RssItem khan1 = createTestRssItem("https://khan.co.kr/1");
            RssItem khan2 = createTestRssItem("https://khan.co.kr/2");
            RssItem kmib1 = createTestRssItem("https://kmib.co.kr/1");

            when(rssReader.readFeed(eq("https://khan.co.kr/rss"), eq("경향신문")))
                    .thenReturn(List.of(khan1, khan2));
            when(rssReader.readFeed(eq("https://kmib.co.kr/rss"), eq("국민일보")))
                    .thenReturn(List.of(kmib1));

            reader.open(new ExecutionContext());

            // when & then
            assertThat(reader.read()).isEqualTo(khan1);
            assertThat(reader.read()).isEqualTo(khan2);
            assertThat(reader.read()).isEqualTo(kmib1);
            assertThat(reader.read()).isNull();
        }

        @Test
        @DisplayName("피드가 비어있으면 null을 반환해야 한다")
        void read_withEmptyFeeds_shouldReturnNull() {
            // given
            when(rssReader.readFeed(anyString(), anyString())).thenReturn(List.of());
            reader.open(new ExecutionContext());

            // when & then
            assertThat(reader.read()).isNull();
        }

        @Test
        @DisplayName("피드 읽기 실패 시 해당 피드만 스킵하고 계속 진행해야 한다")
        void read_withFailedFeed_shouldContinueWithOthers() {
            // given
            RssItem kmib1 = createTestRssItem("https://kmib.co.kr/1");

            when(rssReader.readFeed(eq("https://khan.co.kr/rss"), anyString()))
                    .thenThrow(new RuntimeException("Feed failed"));
            when(rssReader.readFeed(eq("https://kmib.co.kr/rss"), anyString()))
                    .thenReturn(List.of(kmib1));

            reader.open(new ExecutionContext());

            // when & then
            assertThat(reader.read()).isEqualTo(kmib1);
            assertThat(reader.read()).isNull();
        }

        @Test
        @DisplayName("open() 호출 없이 read()하면 null을 반환해야 한다")
        void read_withoutOpen_shouldReturnNull() {
            // when & then
            assertThat(reader.read()).isNull();
        }
    }

    @Nested
    @DisplayName("reset() 메서드")
    class ResetMethod {

        @Test
        @DisplayName("reset() 후 read()는 null을 반환해야 한다")
        void reset_shouldClearIterator() {
            // given
            RssItem item = createTestRssItem("https://example.com/1");
            when(rssReader.readFeed(anyString(), anyString()))
                    .thenReturn(List.of(item));
            
            reader.open(new ExecutionContext());
            assertThat(reader.read()).isNotNull();

            // when
            reader.reset();

            // then
            assertThat(reader.read()).isNull();
        }
    }

    private RssItem createTestRssItem(String url) {
        return RssItem.builder()
                .url(url)
                .title("테스트 제목")
                .description("테스트 설명")
                .publishedAt(LocalDateTime.now())
                .category("테스트")
                .publisher("테스트신문")
                .build();
    }
}
