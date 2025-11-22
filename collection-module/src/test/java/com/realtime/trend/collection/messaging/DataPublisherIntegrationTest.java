package com.realtime.trend.collection.messaging;

import com.realtime.trend.collection.CollectionApplication;
import com.realtime.trend.collection.core.domain.PublishStatus;
import com.realtime.trend.collection.core.messaging.GenericDataPublisher;
import com.realtime.trend.collection.news.domain.News;
import com.realtime.trend.collection.youtube.domain.YouTubeVideo;
import com.realtime.trend.test.config.TestcontainersConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@DisplayName("DataPublisher Kafka 통합 테스트")
@SpringBootTest(classes = CollectionApplication.class)
@ActiveProfiles("test")
@ContextConfiguration(initializers = {
        TestcontainersConfig.MongoInitializer.class,
        TestcontainersConfig.KafkaInitializer.class
})
class DataPublisherIntegrationTest {

    private static final String NEWS_TOPIC = "raw-news";
    private static final String YOUTUBE_TOPIC = "raw-youtube";

    @Autowired
    private GenericDataPublisher dataPublisher;

    private KafkaConsumer<String, Map<String, Object>> consumer;

    @BeforeEach
    void setUp() {
        consumer = createConsumer();
        consumer.subscribe(Arrays.asList(NEWS_TOPIC, YOUTUBE_TOPIC));
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Nested
    @DisplayName("뉴스 발행 테스트")
    class NewsPublishTests {

        @Test
        @DisplayName("뉴스를 Kafka 토픽에 발행할 수 있어야 한다")
        void publishNews_shouldSendToKafkaTopic() {
            // given
            News news = createTestNews("https://example.com/news/" + UUID.randomUUID());

            // when
            dataPublisher.publish(NEWS_TOPIC, news.getUrl(), news);

            // then
            await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    ConsumerRecords<String, Map<String, Object>> records = consumer.poll(Duration.ofMillis(100));
                    List<ConsumerRecord<String, Map<String, Object>>> newsList = new ArrayList<>();
                    records.records(NEWS_TOPIC).forEach(newsList::add);

                    assertThat(newsList).isNotEmpty();
                    Map<String, Object> receivedNews = newsList.get(newsList.size() - 1).value();
                    assertThat(receivedNews.get("url")).isEqualTo(news.getUrl());
                });
        }

        @Test
        @DisplayName("뉴스의 키는 URL이어야 한다")
        void publishNews_shouldUseUrlAsKey() {
            // given
            String expectedKey = "https://example.com/news/key-test-" + UUID.randomUUID();
            News news = createTestNews(expectedKey);

            // when
            dataPublisher.publish(NEWS_TOPIC, news.getUrl(), news);

            // then
            await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ConsumerRecords<String, Map<String, Object>> records = consumer.poll(Duration.ofMillis(100));
                    for (ConsumerRecord<String, Map<String, Object>> record : records.records(NEWS_TOPIC)) {
                        if (expectedKey.equals(record.key())) {
                            assertThat(record.key()).isEqualTo(expectedKey);
                            return;
                        }
                    }
                });
        }
    }

    @Nested
    @DisplayName("YouTube 발행 테스트")
    class YouTubePublishTests {

        @Test
        @DisplayName("YouTube 동영상을 Kafka 토픽에 발행할 수 있어야 한다")
        void publishYouTubeVideo_shouldSendToKafkaTopic() {
            // given
            YouTubeVideo video = createTestYouTubeVideo("video-" + UUID.randomUUID());

            // when
            dataPublisher.publish(YOUTUBE_TOPIC, video.getVideoId(), video);

            // then
            await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    ConsumerRecords<String, Map<String, Object>> records = consumer.poll(Duration.ofMillis(100));
                    List<ConsumerRecord<String, Map<String, Object>>> videoList = new ArrayList<>();
                    records.records(YOUTUBE_TOPIC).forEach(videoList::add);

                    assertThat(videoList).isNotEmpty();
                    Map<String, Object> receivedVideo = videoList.get(videoList.size() - 1).value();
                    assertThat(receivedVideo.get("videoId")).isEqualTo(video.getVideoId());
                });
        }

        @Test
        @DisplayName("YouTube 동영상의 키는 videoId여야 한다")
        void publishYouTubeVideo_shouldUseVideoIdAsKey() {
            // given
            String expectedKey = "video-key-test-" + UUID.randomUUID();
            YouTubeVideo video = createTestYouTubeVideo(expectedKey);

            // when
            dataPublisher.publish(YOUTUBE_TOPIC, video.getVideoId(), video);

            // then
            await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ConsumerRecords<String, Map<String, Object>> records = consumer.poll(Duration.ofMillis(100));
                    for (ConsumerRecord<String, Map<String, Object>> record : records.records(YOUTUBE_TOPIC)) {
                        if (expectedKey.equals(record.key())) {
                            assertThat(record.key()).isEqualTo(expectedKey);
                            return;
                        }
                    }
                });
        }
    }

    // 테스트용 Consumer 생성
    private KafkaConsumer<String, Map<String, Object>> createConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                TestcontainersConfig.KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "java.util.LinkedHashMap");

        return new KafkaConsumer<>(props);
    }

    // 테스트 데이터 생성 헬퍼
    private News createTestNews(String url) {
        return News.builder()
                .url(url)
                .title("테스트 뉴스 제목")
                .content("테스트 뉴스 본문입니다.")
                .publishedAt(LocalDateTime.now())
                .publisher("테스트신문")
                .author("테스트 기자")
                .category("테스트")
                .collectedAt(LocalDateTime.now())
                .publishStatus(PublishStatus.PENDING)
                .build();
    }

    private YouTubeVideo createTestYouTubeVideo(String videoId) {
        return YouTubeVideo.builder()
                .videoId(videoId)
                .title("테스트 유튜브 영상")
                .description("테스트 설명입니다.")
                .channelTitle("테스트 채널")
                .publishedAt(LocalDateTime.now())
                .viewCount(1000L)
                .likeCount(50L)
                .commentCount(10L)
                .collectedAt(LocalDateTime.now())
                .publishStatus(PublishStatus.PENDING)
                .build();
    }
}
