package com.realtime.trend.collection.news.batch;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RSS 피드 설정 프로퍼티
 */
@Getter
@Setter
@Component
public class RssFeedProperties {

    /**
     * RSS 피드 맵 (언론사명 -> URL)
     * 기본값 설정, 필요시 application.yml에서 오버라이드
     */
    private Map<String, String> feeds = new LinkedHashMap<>(Map.of(
            "경향신문", "https://www.khan.co.kr/rss/rssdata/total_news.xml",
            "국민일보", "https://www.kmib.co.kr/rss/data/kmibRssAll.xml"
    ));
}
