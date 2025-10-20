package com.realtime.collectionsystem.youtube;

import com.realtime.collectionsystem.youtube.domain.YoutubeVideo;
import com.realtime.collectionsystem.youtube.repository.YoutubeVideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class YoutubeCollectionIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);

    @Container
    static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("collection_batch")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.host", mongoDBContainer::getHost);
        registry.add("spring.data.mongodb.port", mongoDBContainer::getFirstMappedPort);
        registry.add("spring.data.mongodb.database", () -> "test");

        registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);
    }

    @Autowired
    private YoutubeVideoRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void 유튜브_동영상_저장_및_조회_테스트() {
        YoutubeVideo video = YoutubeVideo.builder()
                .videoId("test123")
                .title("테스트 동영상")
                .description("테스트 설명")
                .tags(List.of("tag1", "tag2"))
                .channelTitle("테스트 채널")
                .categoryId("10")
                .createdDate(LocalDateTime.now())
                .collectedDate(LocalDateTime.now())
                .publishedToKafka(false)
                .build();

        YoutubeVideo saved = repository.save(video);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getVideoId()).isEqualTo("test123");
        assertThat(saved.getPublishedToKafka()).isFalse();
    }

    @Test
    void VideoId_중복_체크_테스트() {
        String videoId = "test123";

        YoutubeVideo video = YoutubeVideo.builder()
                .videoId(videoId)
                .title("테스트 동영상")
                .description("테스트 설명")
                .tags(List.of("tag1", "tag2"))
                .channelTitle("테스트 채널")
                .categoryId("10")
                .createdDate(LocalDateTime.now())
                .collectedDate(LocalDateTime.now())
                .publishedToKafka(false)
                .build();

        repository.save(video);

        boolean exists = repository.existsByVideoId(videoId);
        assertThat(exists).isTrue();
    }

    @Test
    void Kafka_발행_플래그_업데이트_테스트() {
        YoutubeVideo video = YoutubeVideo.builder()
                .videoId("test123")
                .title("테스트 동영상")
                .description("테스트 설명")
                .tags(List.of("tag1", "tag2"))
                .channelTitle("테스트 채널")
                .categoryId("10")
                .createdDate(LocalDateTime.now())
                .collectedDate(LocalDateTime.now())
                .publishedToKafka(false)
                .build();

        YoutubeVideo saved = repository.save(video);
        assertThat(saved.getPublishedToKafka()).isFalse();

        saved.markAsPublished();
        repository.save(saved);

        YoutubeVideo updated = repository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getPublishedToKafka()).isTrue();
    }

    @Test
    void Tags_저장_및_조회_테스트() {
        List<String> tags = List.of("태그1", "태그2", "태그3");

        YoutubeVideo video = YoutubeVideo.builder()
                .videoId("test123")
                .title("테스트 동영상")
                .description("테스트 설명")
                .tags(tags)
                .channelTitle("테스트 채널")
                .categoryId("10")
                .createdDate(LocalDateTime.now())
                .collectedDate(LocalDateTime.now())
                .publishedToKafka(false)
                .build();

        YoutubeVideo saved = repository.save(video);

        assertThat(saved.getTags()).hasSize(3);
        assertThat(saved.getTags()).containsExactlyElementsOf(tags);
    }
}
