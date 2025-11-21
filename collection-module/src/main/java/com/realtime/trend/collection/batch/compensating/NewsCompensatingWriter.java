package com.realtime.trend.collection.batch.compensating;

import com.realtime.trend.collection.domain.News;
import com.realtime.trend.collection.repository.NewsRepository;
import com.realtime.trend.collection.service.kafka.DataPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * 뉴스 보상 트랜잭션 ItemWriter
 * PENDING 상태인 뉴스를 Kafka로 재발행 시도
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewsCompensatingWriter implements ItemWriter<News> {

    private final NewsRepository newsRepository;
    private final DataPublisher dataPublisher;

    @Override
    public void write(Chunk<? extends News> chunk) {
        for (News news : chunk) {
            try {
                // Kafka 재발행 시도
                dataPublisher.publishNews(news);

                // 발행 성공 시 PUBLISHED 상태로 변경
                News publishedNews = news.markAsPublished();
                newsRepository.save(publishedNews);
                log.info("보상 트랜잭션 성공: {}", news.getUrl());

            } catch (Exception e) {
                // 발행 실패 시 PENDING 상태 유지 (다음 실행에서 재시도)
                log.warn("보상 트랜잭션 실패 (재시도 예정): {}", news.getUrl(), e);
            }
        }
    }
}
