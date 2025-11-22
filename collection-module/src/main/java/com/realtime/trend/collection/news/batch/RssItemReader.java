package com.realtime.trend.collection.news.batch;

import com.realtime.trend.collection.news.crawler.RssItem;
import com.realtime.trend.collection.news.crawler.RssReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * RSS 피드 ItemReader
 * 여러 언론사의 RSS를 읽어서 RssItem으로 반환
 */
@Slf4j
@Component
@StepScope
public class RssItemReader implements ItemReader<RssItem> {

    private static final Map<String, String> RSS_FEEDS = Map.of(
            "경향신문", "https://www.khan.co.kr/rss/rssdata/total_news.xml",
            "국민일보", "https://www.kmib.co.kr/rss/data/kmibRssAll.xml"
    );

    private final RssReader rssReader;
    private Iterator<RssItem> itemIterator;
    private boolean initialized = false;

    public RssItemReader(RssReader rssReader) {
        this.rssReader = rssReader;
    }

    @Override
    public RssItem read() {
        if (!initialized) {
            initialize();
            initialized = true;
        }

        if (itemIterator != null && itemIterator.hasNext()) {
            return itemIterator.next();
        }

        return null; // 더 이상 읽을 아이템이 없음
    }

    /**
     * 모든 RSS 피드에서 아이템 수집
     */
    private void initialize() {
        List<RssItem> allItems = new ArrayList<>();

        for (Map.Entry<String, String> entry : RSS_FEEDS.entrySet()) {
            String publisher = entry.getKey();
            String feedUrl = entry.getValue();

            try {
                List<RssItem> items = rssReader.readFeed(feedUrl, publisher);
                allItems.addAll(items);
                log.info("RSS 수집 완료: {} - {}개 기사", publisher, items.size());
            } catch (Exception e) {
                log.error("RSS 수집 실패: {}", publisher, e);
                // 특정 언론사 실패 시 다른 언론사는 계속 수집
            }
        }

        this.itemIterator = allItems.iterator();
        log.info("전체 RSS 수집 완료: {}개 기사", allItems.size());
    }
}
