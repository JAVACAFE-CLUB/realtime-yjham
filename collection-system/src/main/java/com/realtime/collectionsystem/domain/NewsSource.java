package com.realtime.collectionsystem.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum NewsSource {
    KHAN("경향신문", "khan.co.kr", "https://www.khan.co.kr/rss/rssdata/total_news.xml"),
    DONGA("동아일보", "donga.com", "https://rss.donga.com/total.xml");

    private final String displayName;
    private final String domain;
    private final String rssUrl;

    public static NewsSource fromUrl(String url) {
        if (url == null) {
            return null;
        }

        for (NewsSource source : values()) {
            if (url.contains(source.domain)) {
                return source;
            }
        }
        return null;
    }

    public static NewsSource fromRssUrl(String rssUrl) {
        if (rssUrl == null) {
            return null;
        }

        for (NewsSource source : values()) {
            if (source.rssUrl.equals(rssUrl)) {
                return source;
            }
        }
        return null;
    }

    public static NewsSource fromDomain(String domain) {
        if (domain == null) {
            return null;
        }

        for (NewsSource source : values()) {
            if (source.domain.equals(domain)) {
                return source;
            }
        }
        return null;
    }

    public boolean canHandle(String url) {
        return url != null && url.contains(this.domain);
    }

    public static List<String> getAllRssUrls() {
        return Arrays.stream(values())
                .map(NewsSource::getRssUrl)
                .toList();
    }
}