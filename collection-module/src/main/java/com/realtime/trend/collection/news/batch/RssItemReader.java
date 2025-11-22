package com.realtime.trend.collection.news.batch;

import com.realtime.trend.collection.news.crawler.RssItem;
import com.realtime.trend.collection.news.crawler.RssReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RSS 피드 ItemReader
 * 여러 언론사의 RSS를 읽어서 RssItem으로 반환
 */
@Slf4j
@Component
public class RssItemReader implements ItemReader<RssItem> {

    private final RssReader rssReader;
    private final Map<String, String> feeds;
    private Iterator<RssItem> itemIterator;
    private boolean initialized = false;

    public RssItemReader(RssReader rssReader, RssFeedProperties properties) {
        this.rssReader = rssReader;
        this.feeds = properties.getFeeds();
        log.info("RSS 피드 설정 로드: {}개 언론사 - {}", feeds.size(), feeds.keySet());
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

        return null;
    }

    /**
     * 모든 RSS 피드에서 아이템 수집
     */
    private void initialize() {
        List<RssItem> allItems = new ArrayList<>();

        for (Map.Entry<String, String> entry : feeds.entrySet()) {
            String publisher = entry.getKey();
            String feedUrl = entry.getValue();

            try {
                List<RssItem> items = rssReader.readFeed(feedUrl, publisher);
                allItems.addAll(items);
                log.info("RSS 수집 완료: {} - {}개 기사", publisher, items.size());
            } catch (Exception e) {
                log.error("RSS 수집 실패: {}", publisher, e);
            }
        }

        this.itemIterator = allItems.iterator();
        log.info("전체 RSS 수집 완료: {}개 기사", allItems.size());
    }

    /**
     * 상태 초기화 (재사용 대비)
     */
    public void reset() {
        this.initialized = false;
        this.itemIterator = null;
    }
}
