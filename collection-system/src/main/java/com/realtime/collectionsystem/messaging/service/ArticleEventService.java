package com.realtime.collectionsystem.messaging.service;

import com.realtime.collectionsystem.domain.Article;
import com.realtime.collectionsystem.messaging.dto.ArticleCollectionEvent;
import com.realtime.collectionsystem.messaging.producer.ArticleCollectionEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleEventService {

    private final ArticleCollectionEventProducer eventProducer;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final String EVENT_TYPE = "ARTICLE_COLLECTED";
    private static final String VERSION = "1.0";

    public void publishCollectionEvents(List<Article> articles) {
        if (articles == null || articles.isEmpty()) {
            log.warn("발송할 기사가 없습니다.");
            return;
        }

        log.info("기사 수집 이벤트 발송 시작: {}개", articles.size());

        List<ArticleCollectionEvent> events = articles.stream()
                .map(this::convertToEvent)
                .toList();

        eventProducer.sendArticleCollectionEvents(events);
        log.info("기사 수집 이벤트 발송 완료: {}개", events.size());
    }

    public void publishCollectionEvent(Article article) {
        if (article == null) {
            log.warn("발송할 기사가 null입니다.");
            return;
        }

        ArticleCollectionEvent event = convertToEvent(article);
        eventProducer.sendArticleCollectionEvent(event);
        log.debug("단일 기사 수집 이벤트 발송: {}", event.getArticleId());
    }

    private ArticleCollectionEvent convertToEvent(Article article) {
        String articleId = generateArticleId(article);
        int contentLength = article.getContent() != null ? article.getContent().length() : 0;

        return ArticleCollectionEvent.builder()
                .articleId(articleId)
                .url(article.getUrl())
                .title(article.getTitle())
                .source(article.getSource())
                .author(article.getAuthor())
                .publishedDate(article.getPublishedDate())
                .collectedDate(article.getCollectedDate())
                .contentLength(contentLength)
                .eventType(EVENT_TYPE)
                .version(VERSION)
                .build();
    }

    private String generateArticleId(Article article) {
        String datePrefix = article.getCollectedDate().format(DATE_FORMATTER);
        String urlHash = String.valueOf(article.getUrl().hashCode());
        String source = article.getSource().replace(" ", "_");
        return String.format("%s/%s/%s.json", datePrefix, source, urlHash);
    }
}