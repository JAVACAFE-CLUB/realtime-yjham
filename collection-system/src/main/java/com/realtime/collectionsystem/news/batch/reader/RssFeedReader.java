package com.realtime.collectionsystem.news.batch.reader;

import com.realtime.collectionsystem.news.batch.dto.RssFeedItem;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class RssFeedReader implements ItemReader<RssFeedItem> {

    private static final Map<String, String> RSS_FEEDS = Map.of(
            "khan", "https://www.khan.co.kr/rss/rssdata/total_news.xml",
            "donga", "https://rss.donga.com/total.xml"
    );

    private Iterator<RssFeedItem> feedItems;

    @Override
    public RssFeedItem read() {
        if (feedItems == null) {
            feedItems = fetchAllFeedItems().iterator();
        }

        if (feedItems.hasNext()) {
            return feedItems.next();
        }

        return null;
    }

    private List<RssFeedItem> fetchAllFeedItems() {
        List<RssFeedItem> items = new ArrayList<>();

        for (Map.Entry<String, String> entry : RSS_FEEDS.entrySet()) {
            String source = entry.getKey();
            String feedUrl = entry.getValue();

            try {
                log.info("{} RSS Feed 수집 시작: {}", source, feedUrl);
                Document doc = Jsoup.connect(feedUrl).parser(org.jsoup.parser.Parser.xmlParser()).get();
                Elements itemElements = doc.select("item");

                for (Element item : itemElements) {
                    String url = extractUrl(item, source);
                    String category = extractCategory(item, source);
                    String publishDate = extractPublishDate(item, source);

                    items.add(RssFeedItem.builder()
                            .source(source)
                            .url(url)
                            .category(category)
                            .publishDate(publishDate)
                            .build());
                }

                log.info("{} RSS Feed 수집 완료: {} 건", source, itemElements.size());

            } catch (Exception e) {
                log.error("{} RSS Feed 수집 실패: {}", source, feedUrl, e);
            }
        }

        return items;
    }

    private String extractUrl(Element item, String source) {
        if ("khan".equals(source)) {
            return item.selectFirst("link") != null
                    ? item.selectFirst("link").text()
                    : "";
        } else if ("donga".equals(source)) {
            return item.selectFirst("link") != null
                    ? item.selectFirst("link").text()
                    : "";
        }
        return "";
    }

    private String extractCategory(Element item, String source) {
        Element category = item.selectFirst("category");
        return category != null ? category.text() : "";
    }

    private String extractPublishDate(Element item, String source) {
        if ("khan".equals(source)) {
            Element dcDate = item.selectFirst("dc|date");
            return dcDate != null ? dcDate.text() : "";
        } else if ("donga".equals(source)) {
            Element pubDate = item.selectFirst("pubDate");
            return pubDate != null ? pubDate.text() : "";
        }
        return "";
    }
}
