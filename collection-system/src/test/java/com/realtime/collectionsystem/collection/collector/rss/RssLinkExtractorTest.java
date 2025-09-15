package com.realtime.collectionsystem.collection.collector.rss;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RssLinkExtractorTest {

    @Autowired
    private RssLinkExtractor rssLinkExtractor;

    @Test
    void extractArticleLinks() {
        List<String> articleLinks = rssLinkExtractor.extractArticleLinks();

        assertThat(articleLinks).isNotEmpty();
        assertThat(articleLinks).allMatch(link -> link.startsWith("http"));
        assertThat(articleLinks).allMatch(link ->
            link.contains("khan.co.kr") || link.contains("donga.com"));

        System.out.println("총 추출된 링크 수: " + articleLinks.size());

        long khanCount = articleLinks.stream()
            .filter(link -> link.contains("khan.co.kr"))
            .count();
        long dongaCount = articleLinks.stream()
            .filter(link -> link.contains("donga.com"))
            .count();

        System.out.println("경향신문 링크: " + khanCount + "개");
        System.out.println("동아일보 링크: " + dongaCount + "개");

        articleLinks.stream()
            .limit(5)
            .forEach(link -> System.out.println("링크: " + link));
    }
}