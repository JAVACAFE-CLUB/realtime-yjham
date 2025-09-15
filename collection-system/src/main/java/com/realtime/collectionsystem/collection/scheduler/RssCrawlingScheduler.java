package com.realtime.collectionsystem.collection.scheduler;

import com.realtime.collectionsystem.collection.collector.rss.RssArticleCollectionService;
import com.realtime.collectionsystem.domain.Article;
import com.realtime.collectionsystem.messaging.service.ArticleEventService;
import com.realtime.collectionsystem.storage.service.ArticleStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RssCrawlingScheduler {

    private final RssArticleCollectionService rssArticleCollectionService;
    private final ArticleStorageService articleStorageService;
    private final ArticleEventService articleEventService;

    @Scheduled(fixedRate = 3600000) // 1시간 = 3600000ms
    public void scheduledRssCrawling() {
        log.info("예약된 RSS 크롤링 실행");
        executeRssCrawling();
    }

    private void executeRssCrawling() {
        try {
            log.info("RSS 크롤링 시작");

            List<Article> articles = rssArticleCollectionService.collectArticles();
            log.info("총 {}개의 기사 수집 완료", articles.size());

            // MinIO에 기사 저장
            articleStorageService.saveArticles(articles);

            // 카프카로 수집 이벤트 발송
            articleEventService.publishCollectionEvents(articles);

            log.info("RSS 크롤링 완료");
        } catch (Exception e) {
            log.error("RSS 크롤링 중 오류 발생", e);
        }
    }
}