package com.realtime.collectionsystem.news.batch.crawler;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class NewsCrawlerFactory {

    private final Map<String, NewsCrawler> crawlers;

    public NewsCrawlerFactory(List<NewsCrawler> crawlerList) {
        this.crawlers = new HashMap<>();
        for (NewsCrawler crawler : crawlerList) {
            crawlers.put(crawler.getSource(), crawler);
        }
    }

    public NewsCrawler getCrawler(String source) {
        NewsCrawler crawler = crawlers.get(source);
        if (crawler == null) {
            throw new IllegalArgumentException("지원하지 않는 언론사: " + source);
        }
        return crawler;
    }
}
