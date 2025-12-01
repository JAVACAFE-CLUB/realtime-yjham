package com.realtime.trend.test.fixture;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 테스트 데이터 생성 팩토리
 * 각 모듈에서 필요한 테스트 데이터를 생성하는 유틸리티
 */
public class TestDataFactory {

    private TestDataFactory() {
    }

    /**
     * 랜덤 ID 생성
     */
    public static String randomId() {
        return UUID.randomUUID().toString();
    }

    /**
     * 테스트용 뉴스 데이터 (Map 형태)
     */
    public static NewsTestData createNewsTestData() {
        return new NewsTestData(
                randomId(),
                "https://example.com/news/" + randomId(),
                "테스트 뉴스 제목 - " + System.currentTimeMillis(),
                "테스트 뉴스 본문 내용입니다. 한국은행 금리 동결 결정...",
                LocalDateTime.now().minusHours(1),
                "테스트신문",
                "홍길동",
                "경제",
                List.of("금리", "한국은행", "경제")
        );
    }

    /**
     * 테스트용 YouTube 데이터
     */
    public static YouTubeTestData createYouTubeTestData() {
        return new YouTubeTestData(
                randomId(),
                "테스트 유튜브 제목 - " + System.currentTimeMillis(),
                "테스트 유튜브 설명입니다.",
                "테스트채널",
                LocalDateTime.now().minusHours(2),
                1000L,
                50L,
                10L
        );
    }

    /**
     * RSS 피드 XML 생성
     */
    public static String createRssXml(String title, String link, String description) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0">
                    <channel>
                        <title>테스트 RSS 피드</title>
                        <link>https://example.com</link>
                        <description>테스트 RSS 피드</description>
                        <item>
                            <title>%s</title>
                            <link>%s</link>
                            <description>%s</description>
                            <pubDate>Mon, 01 Jan 2024 12:00:00 +0900</pubDate>
                        </item>
                    </channel>
                </rss>
                """.formatted(title, link, description);
    }

    /**
     * 테스트용 HTML 문서 생성
     */
    public static String createNewsHtml(String title, String content, String author) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>%s</title>
                </head>
                <body>
                    <article>
                        <h1 class="article-title">%s</h1>
                        <span class="author">%s</span>
                        <div class="article-content">
                            <p>%s</p>
                        </div>
                    </article>
                </body>
                </html>
                """.formatted(title, title, author, content);
    }

    // 테스트 데이터 레코드들
    public record NewsTestData(
            String id,
            String url,
            String title,
            String content,
            LocalDateTime publishedAt,
            String publisher,
            String author,
            String category,
            List<String> tags
    ) {}

    public record YouTubeTestData(
            String videoId,
            String title,
            String description,
            String channelTitle,
            LocalDateTime publishedAt,
            Long viewCount,
            Long likeCount,
            Long commentCount
    ) {}

    // ========== Kafka 메시지 형식 테스트 데이터 ==========

    /**
     * Raw 뉴스 Kafka 메시지 생성 (collection → processing)
     * LocalDateTime을 ISO-8601 문자열 형식으로 포함
     */
    public static Map<String, Object> createRawNewsKafkaMessage() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> message = new HashMap<>();
        message.put("id", randomId());
        message.put("url", "https://example.com/news/" + randomId());
        message.put("title", "테스트 뉴스 제목");
        message.put("content", "테스트 뉴스 본문 내용입니다. 한국은행 금리 동결 결정...");
        message.put("publishedAt", now.minusHours(1).toString());
        message.put("publisher", "테스트신문");
        message.put("author", "홍길동");
        message.put("category", "경제");
        message.put("tags", List.of("금리", "한국은행"));
        message.put("collectedAt", now.toString());
        return message;
    }

    /**
     * Raw 뉴스 Kafka 메시지 생성 - LocalDateTime 배열 형식
     * Jackson의 기본 직렬화 시 배열 형태로 변환될 수 있음
     */
    public static Map<String, Object> createRawNewsKafkaMessageWithArrayDateTime() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> message = new HashMap<>();
        message.put("id", randomId());
        message.put("url", "https://example.com/news/" + randomId());
        message.put("title", "테스트 뉴스 제목 (배열 형식)");
        message.put("content", "테스트 뉴스 본문 내용입니다.");
        message.put("publishedAt", toDateTimeArray(now.minusHours(1)));
        message.put("publisher", "테스트신문");
        message.put("author", "홍길동");
        message.put("category", "경제");
        message.put("tags", List.of("테스트"));
        message.put("collectedAt", toDateTimeArray(now));
        return message;
    }

    /**
     * Raw YouTube Kafka 메시지 생성 (collection → processing)
     */
    public static Map<String, Object> createRawYoutubeKafkaMessage() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> message = new HashMap<>();
        message.put("id", randomId());
        message.put("videoId", "test_video_" + randomId().substring(0, 8));
        message.put("title", "테스트 유튜브 제목");
        message.put("description", "테스트 유튜브 설명입니다.");
        message.put("channelTitle", "테스트채널");
        message.put("publishedAt", now.minusHours(2).toString());
        message.put("categoryId", "22");
        message.put("tags", List.of("테스트", "유튜브"));
        message.put("viewCount", 1000L);
        message.put("likeCount", 50L);
        message.put("commentCount", 10L);
        message.put("collectedAt", now.toString());
        return message;
    }

    /**
     * Processed 뉴스 Kafka 메시지 생성 (processing → indexing)
     */
    public static Map<String, Object> createProcessedNewsKafkaMessage() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> message = new HashMap<>();
        message.put("id", randomId());
        message.put("url", "https://example.com/news/" + randomId());
        message.put("title", "처리된 뉴스 제목");
        message.put("processedContent", "처리된 뉴스 본문 내용입니다.");
        message.put("publishedAt", now.minusHours(1).toString());
        message.put("publisher", "테스트신문");
        message.put("author", "홍길동");
        message.put("category", "경제");
        message.put("tags", List.of("금리", "한국은행"));
        message.put("collectedAt", now.toString());
        message.put("keywords", List.of(
                Map.of("keyword", "한국은행", "type", "ORGANIZATION"),
                Map.of("keyword", "금리", "type", "TERM")
        ));
        return message;
    }

    /**
     * Processed 뉴스 Kafka 메시지 생성 - LocalDateTime 배열 형식
     */
    public static Map<String, Object> createProcessedNewsKafkaMessageWithArrayDateTime() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> message = new HashMap<>();
        message.put("id", randomId());
        message.put("url", "https://example.com/news/" + randomId());
        message.put("title", "처리된 뉴스 제목 (배열 형식)");
        message.put("processedContent", "처리된 뉴스 본문 내용입니다.");
        message.put("publishedAt", toDateTimeArray(now.minusHours(1)));
        message.put("publisher", "테스트신문");
        message.put("author", "홍길동");
        message.put("category", "경제");
        message.put("tags", List.of("테스트"));
        message.put("collectedAt", toDateTimeArray(now));
        message.put("keywords", List.of(
                Map.of("keyword", "테스트키워드", "type", "TERM")
        ));
        return message;
    }

    /**
     * Processed YouTube Kafka 메시지 생성 (processing → indexing)
     */
    public static Map<String, Object> createProcessedYoutubeKafkaMessage() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> message = new HashMap<>();
        message.put("id", randomId());
        message.put("videoId", "test_video_" + randomId().substring(0, 8));
        message.put("title", "처리된 유튜브 제목");
        message.put("processedDescription", "처리된 유튜브 설명입니다.");
        message.put("channelTitle", "테스트채널");
        message.put("publishedAt", now.minusHours(2).toString());
        message.put("categoryId", "22");
        message.put("tags", List.of("테스트", "유튜브"));
        message.put("viewCount", 1000L);
        message.put("likeCount", 50L);
        message.put("commentCount", 10L);
        message.put("collectedAt", now.toString());
        message.put("keywords", List.of(
                Map.of("keyword", "테스트채널", "type", "ORGANIZATION"),
                Map.of("keyword", "유튜브", "type", "TERM")
        ));
        return message;
    }

    /**
     * LocalDateTime을 배열 형태로 변환 [year, month, day, hour, minute, second]
     * Jackson이 JavaTimeModule 없이 직렬화할 때의 기본 형식
     */
    public static List<Integer> toDateTimeArray(LocalDateTime dateTime) {
        return List.of(
                dateTime.getYear(),
                dateTime.getMonthValue(),
                dateTime.getDayOfMonth(),
                dateTime.getHour(),
                dateTime.getMinute(),
                dateTime.getSecond()
        );
    }

    /**
     * 커스텀 Raw 뉴스 Kafka 메시지 빌더
     */
    public static RawNewsKafkaMessageBuilder rawNewsKafkaMessage() {
        return new RawNewsKafkaMessageBuilder();
    }

    /**
     * 커스텀 Processed 뉴스 Kafka 메시지 빌더
     */
    public static ProcessedNewsKafkaMessageBuilder processedNewsKafkaMessage() {
        return new ProcessedNewsKafkaMessageBuilder();
    }

    // ========== Kafka 메시지 빌더 클래스 ==========

    public static class RawNewsKafkaMessageBuilder {
        private final Map<String, Object> message = new HashMap<>();

        public RawNewsKafkaMessageBuilder() {
            // 기본값 설정
            message.put("id", randomId());
            message.put("url", "https://example.com/news/" + randomId());
            message.put("title", "테스트 뉴스");
            message.put("content", "테스트 본문");
            message.put("publishedAt", LocalDateTime.now().toString());
            message.put("publisher", "테스트신문");
            message.put("collectedAt", LocalDateTime.now().toString());
        }

        public RawNewsKafkaMessageBuilder id(String id) {
            message.put("id", id);
            return this;
        }

        public RawNewsKafkaMessageBuilder url(String url) {
            message.put("url", url);
            return this;
        }

        public RawNewsKafkaMessageBuilder title(String title) {
            message.put("title", title);
            return this;
        }

        public RawNewsKafkaMessageBuilder content(String content) {
            message.put("content", content);
            return this;
        }

        public RawNewsKafkaMessageBuilder publishedAt(LocalDateTime publishedAt) {
            message.put("publishedAt", publishedAt.toString());
            return this;
        }

        public RawNewsKafkaMessageBuilder publishedAtAsArray(LocalDateTime publishedAt) {
            message.put("publishedAt", toDateTimeArray(publishedAt));
            return this;
        }

        public RawNewsKafkaMessageBuilder collectedAt(LocalDateTime collectedAt) {
            message.put("collectedAt", collectedAt.toString());
            return this;
        }

        public RawNewsKafkaMessageBuilder collectedAtAsArray(LocalDateTime collectedAt) {
            message.put("collectedAt", toDateTimeArray(collectedAt));
            return this;
        }

        public RawNewsKafkaMessageBuilder publisher(String publisher) {
            message.put("publisher", publisher);
            return this;
        }

        public RawNewsKafkaMessageBuilder author(String author) {
            message.put("author", author);
            return this;
        }

        public RawNewsKafkaMessageBuilder category(String category) {
            message.put("category", category);
            return this;
        }

        public RawNewsKafkaMessageBuilder tags(List<String> tags) {
            message.put("tags", tags);
            return this;
        }

        public Map<String, Object> build() {
            return new HashMap<>(message);
        }
    }

    public static class ProcessedNewsKafkaMessageBuilder {
        private final Map<String, Object> message = new HashMap<>();

        public ProcessedNewsKafkaMessageBuilder() {
            message.put("id", randomId());
            message.put("url", "https://example.com/news/" + randomId());
            message.put("title", "처리된 뉴스");
            message.put("processedContent", "처리된 본문");
            message.put("publishedAt", LocalDateTime.now().toString());
            message.put("publisher", "테스트신문");
            message.put("collectedAt", LocalDateTime.now().toString());
            message.put("keywords", List.of());
        }

        public ProcessedNewsKafkaMessageBuilder id(String id) {
            message.put("id", id);
            return this;
        }

        public ProcessedNewsKafkaMessageBuilder url(String url) {
            message.put("url", url);
            return this;
        }

        public ProcessedNewsKafkaMessageBuilder title(String title) {
            message.put("title", title);
            return this;
        }

        public ProcessedNewsKafkaMessageBuilder processedContent(String content) {
            message.put("processedContent", content);
            return this;
        }

        public ProcessedNewsKafkaMessageBuilder collectedAt(LocalDateTime collectedAt) {
            message.put("collectedAt", collectedAt.toString());
            return this;
        }

        public ProcessedNewsKafkaMessageBuilder collectedAtAsArray(LocalDateTime collectedAt) {
            message.put("collectedAt", toDateTimeArray(collectedAt));
            return this;
        }

        public ProcessedNewsKafkaMessageBuilder keywords(List<Map<String, String>> keywords) {
            message.put("keywords", keywords);
            return this;
        }

        public ProcessedNewsKafkaMessageBuilder addKeyword(String keyword, String type) {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> keywords = (List<Map<String, String>>) message.get("keywords");
            List<Map<String, String>> newKeywords = new java.util.ArrayList<>(keywords);
            newKeywords.add(Map.of("keyword", keyword, "type", type));
            message.put("keywords", newKeywords);
            return this;
        }

        public Map<String, Object> build() {
            return new HashMap<>(message);
        }
    }
}
