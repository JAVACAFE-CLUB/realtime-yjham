package com.realtime.collectionsystem.news;

import com.realtime.collectionsystem.news.domain.NewsArticle;
import com.realtime.collectionsystem.news.repository.NewsArticleRepository;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class NewsCollectionIntegrationTest {

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
    private NewsArticleRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void 뉴스_저장_및_조회_테스트() {
        NewsArticle article = NewsArticle.builder()
                .source("khan")
                .title("테스트 기사")
                .text("테스트 본문")
                .url("https://test.com/article/1")
                .category("정치")
                .createdDate(LocalDateTime.now())
                .collectedDate(LocalDateTime.now())
                .publishedToKafka(false)
                .build();

        NewsArticle saved = repository.save(article);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSource()).isEqualTo("khan");
        assertThat(saved.getPublishedToKafka()).isFalse();
    }

    @Test
    void URL_중복_체크_테스트() {
        String url = "https://test.com/article/1";

        NewsArticle article = NewsArticle.builder()
                .source("khan")
                .title("테스트 기사")
                .text("테스트 본문")
                .url(url)
                .category("정치")
                .createdDate(LocalDateTime.now())
                .collectedDate(LocalDateTime.now())
                .publishedToKafka(false)
                .build();

        repository.save(article);

        boolean exists = repository.existsByUrl(url);
        assertThat(exists).isTrue();
    }

    @Test
    void Kafka_발행_플래그_업데이트_테스트() {
        NewsArticle article = NewsArticle.builder()
                .source("khan")
                .title("테스트 기사")
                .text("테스트 본문")
                .url("https://test.com/article/1")
                .category("정치")
                .createdDate(LocalDateTime.now())
                .collectedDate(LocalDateTime.now())
                .publishedToKafka(false)
                .build();

        NewsArticle saved = repository.save(article);
        assertThat(saved.getPublishedToKafka()).isFalse();

        saved.markAsPublished();
        repository.save(saved);

        NewsArticle updated = repository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getPublishedToKafka()).isTrue();
    }
}
