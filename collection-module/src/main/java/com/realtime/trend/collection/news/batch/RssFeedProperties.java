package com.realtime.trend.collection.news.batch;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RSS 피드 설정 프로퍼티
 * 기본 피드 설정 + 환경변수로 추가 피드 설정 가능
 */
@Getter
@Setter
@Component
public class RssFeedProperties {

    /**
     * RSS 피드 맵 (언론사명 -> URL)
     */
    private Map<String, String> feeds = new LinkedHashMap<>();

    /**
     * 추가 피드 (환경변수로 설정 가능, 형식: "언론사1=URL1,언론사2=URL2")
     */
    @Value("${collection.news.rss.additional-feeds:}")
    private String additionalFeeds;

    @PostConstruct
    public void init() {
        // 기본 피드 설정
        feeds.put("경향신문", "https://www.khan.co.kr/rss/rssdata/total_news.xml");
        feeds.put("국민일보", "https://www.kmib.co.kr/rss/data/kmibRssAll.xml");

        // 추가 피드 파싱
        if (additionalFeeds != null && !additionalFeeds.isBlank()) {
            for (String entry : additionalFeeds.split(",")) {
                String[] parts = entry.trim().split("=", 2);
                if (parts.length == 2) {
                    feeds.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
    }
}
