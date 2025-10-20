package com.realtime.collectionsystem.wiki;

import com.realtime.collectionsystem.wiki.domain.WikiPage;
import com.realtime.collectionsystem.wiki.repository.WikiPageRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class WikiCollectionIntegrationTest {

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
    private WikiPageRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void 위키페이지_저장_및_조회_테스트() {
        WikiPage page = WikiPage.builder()
                .pageId(5L)
                .revisionId(40301269L)
                .title("테스트 페이지")
                .text("테스트 본문")
                .createdDate(LocalDateTime.now())
                .collectedDate(LocalDateTime.now())
                .publishedToKafka(false)
                .build();

        WikiPage saved = repository.save(page);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("테스트 페이지");
        assertThat(saved.getPublishedToKafka()).isFalse();
    }

    @Test
    void Title_중복_체크_테스트() {
        String title = "테스트 페이지";

        WikiPage page = WikiPage.builder()
                .pageId(5L)
                .revisionId(40301269L)
                .title(title)
                .text("테스트 본문")
                .createdDate(LocalDateTime.now())
                .collectedDate(LocalDateTime.now())
                .publishedToKafka(false)
                .build();

        repository.save(page);

        Optional<WikiPage> found = repository.findByTitle(title);
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo(title);
    }

    @Test
    void RevisionId_업데이트_테스트() {
        WikiPage page = WikiPage.builder()
                .pageId(5L)
                .revisionId(1L)
                .title("테스트 페이지")
                .text("원본 본문")
                .createdDate(LocalDateTime.now())
                .collectedDate(LocalDateTime.now())
                .publishedToKafka(false)
                .build();

        WikiPage saved = repository.save(page);

        saved.updateContent(2L, "업데이트된 본문", LocalDateTime.now(), LocalDateTime.now());
        repository.save(saved);

        WikiPage updated = repository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getRevisionId()).isEqualTo(2L);
        assertThat(updated.getText()).isEqualTo("업데이트된 본문");
        assertThat(updated.getPublishedToKafka()).isFalse();
    }

    @Test
    void Kafka_발행_플래그_업데이트_테스트() {
        WikiPage page = WikiPage.builder()
                .pageId(5L)
                .revisionId(40301269L)
                .title("테스트 페이지")
                .text("테스트 본문")
                .createdDate(LocalDateTime.now())
                .collectedDate(LocalDateTime.now())
                .publishedToKafka(false)
                .build();

        WikiPage saved = repository.save(page);
        assertThat(saved.getPublishedToKafka()).isFalse();

        saved.markAsPublished();
        repository.save(saved);

        WikiPage updated = repository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getPublishedToKafka()).isTrue();
    }
}
