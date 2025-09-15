package com.realtime.collectionsystem.collection.collector.rss;

import com.realtime.collectionsystem.domain.NewsSource;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Component
public class RssLinkExtractor {

    public List<String> extractArticleLinks() {
        return NewsSource.getAllRssUrls().stream()
                .flatMap(this::extractLinksFromRss)
                .filter(link -> link != null && !link.isEmpty())
                .distinct()
                .toList();
    }

    private Stream<String> extractLinksFromRss(String rssUrl) {
        try {
            log.debug("RSS 피드 처리 시작: {}", rssUrl);

            SyndFeedInput input = new SyndFeedInput();
            URI uri = URI.create(rssUrl);

            XmlReader xmlReader = new XmlReader(uri.toURL().openStream());
            SyndFeed feed = input.build(xmlReader);

            List<String> links = feed.getEntries().stream()
                    .map(SyndEntry::getLink)
                    .filter(link -> link != null && !link.isEmpty())
                    .toList();

            log.info("RSS 피드에서 {}개 링크 추출 완료: {}", links.size(), getSourceName(rssUrl));
            return links.stream();

        } catch (Exception e) {
            log.error("RSS 피드 처리 실패: {}", rssUrl, e);
            return Stream.empty();
        }
    }

    private String getSourceName(String rssUrl) {
        NewsSource source = NewsSource.fromRssUrl(rssUrl);
        return source != null ? source.getDisplayName() : "알 수 없음";
    }
}