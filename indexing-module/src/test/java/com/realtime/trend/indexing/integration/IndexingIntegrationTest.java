package com.realtime.trend.indexing.integration;

import com.realtime.trend.indexing.document.KeywordDocument;
import com.realtime.trend.indexing.repository.KeywordRepository;
import com.realtime.trend.test.config.AbstractKafkaElasticsearchRedisIntegrationTest;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class IndexingIntegrationTest extends AbstractKafkaElasticsearchRedisIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private KeywordRepository keywordRepository;

    @Value("${kafka.topics.processed-news}")
    private String processedNewsTopic;

    @Value("${kafka.topics.processed-youtube}")
    private String processedYoutubeTopic;

    @BeforeEach
    void setUp() {
        keywordRepository.deleteAll();
    }

    @Test
    @DisplayName("처리된 뉴스 메시지를 수신하여 Elasticsearch에 인덱싱한다")
    void consumeProcessedNews_indexesToElasticsearch() {
        // given
        Map<String, Object> newsMessage = Map.of(
                "id", "news-integration-001",
                "url", "https://example.com/news/1",
                "title", "삼성전자 주가 급등",
                "processedContent", "삼성전자 관련 뉴스",
                "collectedAt", List.of(2024, 1, 15, 10, 30, 0),
                "keywords", List.of(
                        Map.of("keyword", "삼성전자", "type", "ORG"),
                        Map.of("keyword", "주가", "type", "TERM")
                )
        );

        // when
        kafkaTemplate.send(new ProducerRecord<>(processedNewsTopic, "key1", newsMessage));

        // then
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            Iterable<KeywordDocument> documents = keywordRepository.findAll();
            List<KeywordDocument> documentList = toList(documents);

            assertThat(documentList).hasSizeGreaterThanOrEqualTo(2);
            assertThat(documentList)
                    .extracting(KeywordDocument::getKeyword)
                    .contains("삼성전자", "주가");
            assertThat(documentList)
                    .extracting(KeywordDocument::getSource)
                    .containsOnly("news");
        });
    }

    @Test
    @DisplayName("처리된 YouTube 메시지를 수신하여 Elasticsearch에 인덱싱한다")
    void consumeProcessedYoutube_indexesToElasticsearch() {
        // given
        Map<String, Object> youtubeMessage = Map.of(
                "id", "youtube-integration-001",
                "videoId", "video123abc",
                "title", "BTS 신곡 발표",
                "processedDescription", "BTS 관련 영상",
                "collectedAt", List.of(2024, 2, 20, 14, 0, 0),
                "keywords", List.of(
                        Map.of("keyword", "BTS", "type", "ORG")
                )
        );

        // when
        kafkaTemplate.send(new ProducerRecord<>(processedYoutubeTopic, "key2", youtubeMessage));

        // then
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            Iterable<KeywordDocument> documents = keywordRepository.findAll();
            List<KeywordDocument> documentList = toList(documents);

            assertThat(documentList)
                    .extracting(KeywordDocument::getKeyword)
                    .contains("BTS");
            assertThat(documentList)
                    .filteredOn(d -> "BTS".equals(d.getKeyword()))
                    .extracting(KeywordDocument::getSource)
                    .containsOnly("youtube");
        });
    }

    @Test
    @DisplayName("여러 메시지를 순차적으로 처리한다")
    void consumeMultipleMessages_indexesAll() {
        // given
        Map<String, Object> newsMessage = Map.of(
                "id", "news-multi-001",
                "url", "https://example.com/news/multi",
                "title", "뉴스 제목",
                "processedContent", "뉴스 내용",
                "collectedAt", List.of(2024, 1, 15, 10, 30, 0),
                "keywords", List.of(
                        Map.of("keyword", "키워드A", "type", "ORG")
                )
        );

        Map<String, Object> youtubeMessage = Map.of(
                "id", "youtube-multi-001",
                "videoId", "videoMulti",
                "title", "영상 제목",
                "processedDescription", "영상 설명",
                "collectedAt", List.of(2024, 2, 20, 14, 0, 0),
                "keywords", List.of(
                        Map.of("keyword", "키워드B", "type", "PER")
                )
        );

        // when
        kafkaTemplate.send(new ProducerRecord<>(processedNewsTopic, "key3", newsMessage));
        kafkaTemplate.send(new ProducerRecord<>(processedYoutubeTopic, "key4", youtubeMessage));

        // then
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            Iterable<KeywordDocument> documents = keywordRepository.findAll();
            List<KeywordDocument> documentList = toList(documents);

            assertThat(documentList)
                    .extracting(KeywordDocument::getKeyword)
                    .contains("키워드A", "키워드B");
        });
    }

    @Test
    @DisplayName("키워드가 없는 메시지는 인덱싱하지 않는다")
    void consumeMessageWithoutKeywords_doesNotIndex() throws InterruptedException {
        // given - 먼저 빈 상태 확인
        long initialCount = keywordRepository.count();

        Map<String, Object> messageWithoutKeywords = Map.of(
                "id", "news-no-keywords",
                "url", "https://example.com/news/empty",
                "title", "키워드 없는 뉴스",
                "processedContent", "내용",
                "collectedAt", List.of(2024, 1, 15, 10, 30, 0),
                "keywords", List.of()
        );

        // when
        kafkaTemplate.send(new ProducerRecord<>(processedNewsTopic, "key5", messageWithoutKeywords));

        // then - 잠시 대기 후 카운트 확인
        Thread.sleep(3000);
        long finalCount = keywordRepository.count();
        assertThat(finalCount).isEqualTo(initialCount);
    }

    private <T> List<T> toList(Iterable<T> iterable) {
        java.util.ArrayList<T> list = new java.util.ArrayList<>();
        iterable.forEach(list::add);
        return list;
    }
}
