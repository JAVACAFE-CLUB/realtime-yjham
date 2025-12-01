package com.realtime.trend.serving.integration;

import com.realtime.trend.test.config.AbstractElasticsearchRedisIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class ServingIntegrationTest extends AbstractElasticsearchRedisIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Value("${elasticsearch.index.keywords}")
    private String indexName;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @BeforeEach
    void setUp() {
        // 인덱스 초기화
        IndexCoordinates indexCoordinates = IndexCoordinates.of(indexName);
        IndexOperations indexOps = elasticsearchOperations.indexOps(indexCoordinates);

        if (indexOps.exists()) {
            indexOps.delete();
        }
        indexOps.create();

        // 테스트 데이터 삽입
        insertTestKeyword("삼성전자", "ORGANIZATION", "news", "news-001");
        insertTestKeyword("삼성전자", "ORGANIZATION", "news", "news-002");
        insertTestKeyword("이재용", "PERSON", "news", "news-003");
        insertTestKeyword("BTS", "ORGANIZATION", "youtube", "video-001");
        insertTestKeyword("BTS", "ORGANIZATION", "youtube", "video-002");
        insertTestKeyword("BTS", "ORGANIZATION", "youtube", "video-003");

        // 인덱스 리프레시
        indexOps.refresh();
    }

    @Test
    @DisplayName("REST API로 오늘의 키워드를 조회한다")
    void getTodayKeywords_returnsKeywordsFromElasticsearch() throws Exception {
        mockMvc.perform(get("/api/keywords/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keywords").isArray())
                .andExpect(jsonPath("$.metadata.source").value("all"))
                .andExpect(jsonPath("$.metadata.type").value("all"));
    }

    @Test
    @DisplayName("source 파라미터로 뉴스만 조회한다")
    void getTodayKeywords_withNewsSource_returnsNewsKeywords() throws Exception {
        mockMvc.perform(get("/api/keywords/today")
                        .param("source", "news"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.source").value("news"));
    }

    @Test
    @DisplayName("source 파라미터로 YouTube만 조회한다")
    void getTodayKeywords_withYoutubeSource_returnsYoutubeKeywords() throws Exception {
        mockMvc.perform(get("/api/keywords/today")
                        .param("source", "youtube"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.source").value("youtube"));
    }

    @Test
    @DisplayName("type 파라미터로 조직만 조회한다")
    void getTodayKeywords_withOrgType_returnsOrgKeywords() throws Exception {
        mockMvc.perform(get("/api/keywords/today")
                        .param("type", "ORGANIZATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.type").value("ORGANIZATION"));
    }

    @Test
    @DisplayName("limit 파라미터로 결과 수를 제한한다")
    void getTodayKeywords_withLimit_limitsResults() throws Exception {
        mockMvc.perform(get("/api/keywords/today")
                        .param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.limit").value(1));
    }

    @Test
    @DisplayName("유효하지 않은 limit은 400 에러를 반환한다")
    void getTodayKeywords_withInvalidLimit_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/keywords/today")
                        .param("limit", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("유효하지 않은 source는 400 에러를 반환한다")
    void getTodayKeywords_withInvalidSource_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/keywords/today")
                        .param("source", "invalid"))
                .andExpect(status().isBadRequest());
    }

    private void insertTestKeyword(String keyword, String type, String source, String sourceId) {
        Map<String, Object> document = new HashMap<>();
        document.put("keyword", keyword);
        document.put("type", type);
        document.put("source", source);
        document.put("sourceId", sourceId);
        document.put("collectedAt", LocalDateTime.now().format(DATE_FORMATTER));
        document.put("originalText", "테스트 원문");

        IndexQuery indexQuery = new IndexQueryBuilder()
                .withObject(document)
                .build();

        elasticsearchOperations.index(indexQuery, IndexCoordinates.of(indexName));
    }
}
