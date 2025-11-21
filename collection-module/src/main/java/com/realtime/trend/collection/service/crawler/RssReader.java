package com.realtime.trend.collection.service.crawler;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * RSS 피드 읽기
 */
@Slf4j
@Component
public class RssReader {

    /**
     * RSS 피드에서 아이템 목록 조회
     */
    public List<RssItem> readFeed(String feedUrl, String publisher) {
        List<RssItem> items = new ArrayList<>();

        try {
            URL url = new URL(feedUrl);
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(url));

            for (SyndEntry entry : feed.getEntries()) {
                RssItem item = RssItem.builder()
                        .url(cleanUrl(entry.getLink()))
                        .title(entry.getTitle())
                        .description(extractDescription(entry))
                        .publishedAt(convertToLocalDateTime(entry.getPublishedDate()))
                        .category(extractCategory(entry))
                        .publisher(publisher)
                        .build();

                items.add(item);
            }

            log.info("RSS 피드 읽기 성공: {} - {}개 아이템", publisher, items.size());

        } catch (FeedException | IOException e) {
            log.error("RSS 피드 읽기 실패: {} - {}", publisher, feedUrl, e);
        }

        return items;
    }

    /**
     * URL 정리 (UTM 파라미터만 제거)
     */
    private String cleanUrl(String url) {
        if (url == null) {
            return null;
        }
        // UTM 파라미터 등 추적 파라미터만 제거
        return url.replaceAll("[?&](utm_[^&]*|fbclid[^&]*|gclid[^&]*)", "")
                  .replaceAll("\\?&", "?")  // ?& 를 ? 로
                  .replaceAll("&&", "&");    // && 를 & 로
    }

    /**
     * 설명 추출
     */
    private String extractDescription(SyndEntry entry) {
        if (entry.getDescription() != null) {
            return entry.getDescription().getValue();
        }
        return "";
    }

    /**
     * 카테고리 추출
     */
    private String extractCategory(SyndEntry entry) {
        if (entry.getCategories() != null && !entry.getCategories().isEmpty()) {
            return entry.getCategories().get(0).getName();
        }
        return "";
    }

    /**
     * Date를 LocalDateTime으로 변환
     */
    private LocalDateTime convertToLocalDateTime(Date date) {
        if (date == null) {
            return LocalDateTime.now();
        }
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}
