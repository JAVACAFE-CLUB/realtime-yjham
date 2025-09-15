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
        log.info("[RSS-CRAWLING] 예약된 크롤링 실행 시작");
        executeRssCrawling();
    }

    private void executeRssCrawling() {
        long startTime = System.currentTimeMillis();
        try {
            log.info("[RSS-CRAWLING] 기사 수집 시작");

            List<Article> articles = rssArticleCollectionService.collectArticles();

            if (articles.isEmpty()) {
                log.warn("[RSS-CRAWLING] 수집된 기사가 없습니다");
                return;
            }

            log.info("[RSS-CRAWLING] 기사 수집 완료 - 총 {}개", articles.size());

            // MinIO에 기사 저장
            articleStorageService.saveArticles(articles);

            // 카프카로 수집 이벤트 발송
            articleEventService.publishCollectionEvents(articles);

            long duration = System.currentTimeMillis() - startTime;
            log.info("[RSS-CRAWLING] 전체 프로세스 완료 - 소요시간: {}ms", duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[RSS-CRAWLING] 크롤링 실패 - 소요시간: {}ms, 오류: {}", duration, e.getMessage(), e);
        }
    }
}