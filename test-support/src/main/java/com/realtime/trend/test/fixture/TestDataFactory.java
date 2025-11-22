package com.realtime.trend.test.fixture;

import java.time.LocalDateTime;
import java.util.List;
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
}
