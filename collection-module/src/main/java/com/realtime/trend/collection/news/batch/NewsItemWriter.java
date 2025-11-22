package com.realtime.trend.collection.news.batch;

import com.realtime.trend.collection.messaging.DataPublisher;
import com.realtime.trend.collection.news.domain.News;
import com.realtime.trend.collection.news.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * 뉴스 ItemWriter
 * MongoDB 저장 및 Kafka 발행 (Best Effort + Compensating Transaction 패턴)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewsItemWriter implements ItemWriter<News> {

    private final NewsRepository newsRepository;
    private final DataPublisher dataPublisher;

    @Override
    public void write(Chunk<? extends News> chunk) {
        for (News news : chunk) {
            try {
                // 1. MongoDB 저장 (PENDING 상태)
                News savedNews = newsRepository.save(news);
                log.debug("뉴스 저장 완료: {}", savedNews.getUrl());

                // 2. Kafka 발행 시도
                try {
                    dataPublisher.publishNews(savedNews);

                    // 3. 발행 성공 시 PUBLISHED 상태로 변경
                    News publishedNews = savedNews.markAsPublished();
                    newsRepository.save(publishedNews);
                    log.debug("뉴스 발행 완료: {}", publishedNews.getUrl());

                } catch (Exception e) {
                    // Kafka 발행 실패 시 PENDING 상태 유지 (보상 트랜잭션에서 재시도)
                    log.warn("Kafka 발행 실패 (PENDING 상태 유지): {}", savedNews.getUrl(), e);
                }

            } catch (Exception e) {
                log.error("뉴스 저장 실패: {}", news.getUrl(), e);
                // MongoDB 저장 실패는 해당 아이템만 스킵하고 계속 진행
            }
        }
    }
}
