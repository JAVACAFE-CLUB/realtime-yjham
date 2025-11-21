package com.realtime.trend.collection.batch.compensating;

import com.realtime.trend.collection.domain.News;
import com.realtime.trend.collection.domain.PublishStatus;
import com.realtime.trend.collection.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;

/**
 * 뉴스 보상 트랜잭션 ItemReader
 * PENDING 상태인 뉴스를 조회
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewsCompensatingReader implements ItemReader<News> {

    private static final int RETRY_AFTER_MINUTES = 60; // 1시간 후 재시도

    private final NewsRepository newsRepository;
    private Iterator<News> newsIterator;
    private boolean initialized = false;

    @Override
    public News read() {
        if (!initialized) {
            initialize();
            initialized = true;
        }

        if (newsIterator != null && newsIterator.hasNext()) {
            return newsIterator.next();
        }

        return null;
    }

    /**
     * PENDING 상태인 뉴스 조회
     */
    private void initialize() {
        LocalDateTime retryThreshold = LocalDateTime.now().minusMinutes(RETRY_AFTER_MINUTES);
        List<News> pendingNews = newsRepository
                .findByPublishStatusAndCollectedAtBefore(PublishStatus.PENDING, retryThreshold);

        this.newsIterator = pendingNews.iterator();
        log.info("보상 트랜잭션 대상 뉴스: {}개", pendingNews.size());
    }
}
