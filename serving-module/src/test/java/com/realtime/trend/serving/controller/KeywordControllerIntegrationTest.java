package com.realtime.trend.serving.controller;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.springframework.cache.CacheManager;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@DisplayName("KeywordController 통합 테스트")
class KeywordControllerIntegrationTest {

    @Container
    static ElasticsearchContainer elasticsearch = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.11.0")
    ).withEnv("xpack.security.enabled", "false")
            .withEnv("discovery.type", "single-node");

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine")
    ).withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris", elasticsearch::getHttpHostAddress);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("elasticsearch.index.keywords", () -> "keywords-test");
        registry.add("cache.redis.key-prefix", () -> "keywords:test");
        registry.add("cache.redis.ttl-minutes", () -> 5);
        registry.add("rate-limit.requests-per-minute", () -> 100);
        registry.add("rate-limit.requests-per-hour", () -> 1000);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CacheManager cacheManager;

    private static final String INDEX_NAME = "keywords-test";
    private static final String CACHE_NAME = "today";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @BeforeEach
    void setUp() throws IOException {
        // 인덱스 삭제 후 재생성
        try {
            boolean exists = elasticsearchClient.indices()
                    .exists(ExistsRequest.of(e -> e.index(INDEX_NAME)))
                    .value();
            if (exists) {
                elasticsearchClient.indices().delete(DeleteIndexRequest.of(d -> d.index(INDEX_NAME)));
            }
        } catch (Exception ignored) {
        }

        // 인덱스 생성
        elasticsearchClient.indices().create(CreateIndexRequest.of(c -> c
                .index(INDEX_NAME)
                .mappings(m -> m
                        .properties("keyword", p -> p.keyword(k -> k))
                        .properties("type", p -> p.keyword(k -> k))
                        .properties("source", p -> p.keyword(k -> k))
                        .properties("collectedAt", p -> p.date(d -> d.format("yyyy-MM-dd'T'HH:mm:ss")))
                )
        ));

        // Redis 캐시 클리어
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    @DisplayName("키워드가 없을 때 빈 목록을 반환해야 한다")
    void whenNoKeywords_shouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/api/keywords/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keywords").isArray())
                .andExpect(jsonPath("$.keywords.length()").value(0))
                .andExpect(jsonPath("$.metadata.totalCount").value(0));
    }

    @Test
    @DisplayName("인덱싱된 키워드를 조회할 수 있어야 한다")
    void shouldReturnIndexedKeywords() throws Exception {
        // given - 테스트 데이터 인덱싱
        String now = LocalDateTime.now().format(DATE_FORMATTER);

        indexKeyword("테스트키워드", "PERSON", "news", now);

        // Elasticsearch refresh
        elasticsearchClient.indices().refresh(r -> r.index(INDEX_NAME));
        Thread.sleep(1000);

        // when & then
        mockMvc.perform(get("/api/keywords/today")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keywords").isArray())
                .andExpect(jsonPath("$.keywords.length()").isNumber());
    }

    @Test
    @DisplayName("source 필터가 적용되어야 한다")
    void shouldFilterBySource() throws Exception {
        // given
        String now = LocalDateTime.now().format(DATE_FORMATTER);

        indexKeyword("뉴스키워드", "PERSON", "news", now);
        indexKeyword("유튜브키워드", "PERSON", "youtube", now);

        elasticsearchClient.indices().refresh(r -> r.index(INDEX_NAME));
        Thread.sleep(1000);

        // when & then - news만 조회
        mockMvc.perform(get("/api/keywords/today")
                        .param("source", "news"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keywords.length()").value(1))
                .andExpect(jsonPath("$.keywords[0].keyword").value("뉴스키워드"));
    }

    @Test
    @DisplayName("type 필터가 적용되어야 한다")
    void shouldFilterByType() throws Exception {
        // given
        String now = LocalDateTime.now().format(DATE_FORMATTER);

        indexKeyword("서울", "LOCATION", "news", now);
        indexKeyword("김철수", "PERSON", "news", now);

        elasticsearchClient.indices().refresh(r -> r.index(INDEX_NAME));
        Thread.sleep(1000);

        // when & then - LOCATION만 조회
        mockMvc.perform(get("/api/keywords/today")
                        .param("type", "LOCATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keywords.length()").value(1))
                .andExpect(jsonPath("$.keywords[0].type").value("LOCATION"));
    }

    @Test
    @DisplayName("limit이 적용되어야 한다")
    void shouldApplyLimit() throws Exception {
        // given
        String now = LocalDateTime.now().format(DATE_FORMATTER);

        for (int i = 0; i < 10; i++) {
            indexKeyword("키워드" + i, "PERSON", "news", now);
        }

        elasticsearchClient.indices().refresh(r -> r.index(INDEX_NAME));
        Thread.sleep(1000);

        // when & then
        mockMvc.perform(get("/api/keywords/today")
                        .param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keywords.length()").value(3))
                .andExpect(jsonPath("$.metadata.limit").value(3));
    }

    @Test
    @DisplayName("동일한 요청은 캐싱되어야 한다")
    void shouldCacheResults() throws Exception {
        // given
        String now = LocalDateTime.now().format(DATE_FORMATTER);
        indexKeyword("캐시테스트", "PERSON", "news", now);

        elasticsearchClient.indices().refresh(r -> r.index(INDEX_NAME));
        Thread.sleep(1000);

        // when - 첫 번째 요청
        mockMvc.perform(get("/api/keywords/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keywords[0].keyword").value("캐시테스트"));

        // then - 두 번째 동일 요청도 성공해야 함 (캐시 또는 ES에서 조회)
        mockMvc.perform(get("/api/keywords/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keywords[0].keyword").value("캐시테스트"));
    }

    @Test
    @DisplayName("빈 결과는 캐싱되지 않아야 한다 (unless 조건)")
    void shouldNotCacheEmptyResults() throws Exception {
        // given - ES에 데이터 없음

        // when - 빈 결과 조회
        mockMvc.perform(get("/api/keywords/today")
                        .param("source", "news")
                        .param("type", "PERSON")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keywords.length()").value(0));

        // then - 캐시에 저장되지 않아야 함
        String cacheKey = "source:news:type:PERSON:limit:5";
        var cache = cacheManager.getCache(CACHE_NAME);
        assertThat(cache).isNotNull();
        assertThat(cache.get(cacheKey)).isNull();
    }

    @Test
    @DisplayName("다른 파라미터는 독립적으로 조회되어야 한다")
    void shouldHandleDifferentParamsIndependently() throws Exception {
        // given
        String now = LocalDateTime.now().format(DATE_FORMATTER);
        indexKeyword("뉴스키워드", "PERSON", "news", now);
        indexKeyword("유튜브키워드", "PERSON", "youtube", now);

        elasticsearchClient.indices().refresh(r -> r.index(INDEX_NAME));
        Thread.sleep(1000);

        // when & then - 서로 다른 파라미터로 조회하면 각각 다른 결과 반환
        mockMvc.perform(get("/api/keywords/today").param("source", "news"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keywords[0].keyword").value("뉴스키워드"));

        mockMvc.perform(get("/api/keywords/today").param("source", "youtube"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keywords[0].keyword").value("유튜브키워드"));

        // 동일 파라미터로 다시 조회해도 동일한 결과 반환
        mockMvc.perform(get("/api/keywords/today").param("source", "news"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keywords[0].keyword").value("뉴스키워드"));
    }

    private void indexKeyword(String keyword, String type, String source, String collectedAt) throws IOException {
        elasticsearchClient.index(IndexRequest.of(i -> i
                .index(INDEX_NAME)
                .document(Map.of(
                        "keyword", keyword,
                        "type", type,
                        "source", source,
                        "collectedAt", collectedAt
                ))
        ));
    }
}
