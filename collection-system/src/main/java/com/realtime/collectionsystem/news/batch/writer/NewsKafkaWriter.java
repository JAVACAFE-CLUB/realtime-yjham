package com.realtime.collectionsystem.news.batch.writer;

import com.realtime.collectionsystem.common.event.NewsCollectionEvent;
import com.realtime.collectionsystem.news.domain.NewsArticle;
import com.realtime.collectionsystem.news.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsKafkaWriter implements ItemWriter<NewsArticle> {

    private static final String TOPIC = "news-collect-topic";
    private static final String MEDIA_HEADER = "media";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final NewsArticleRepository repository;

    @Override
    public void write(Chunk<? extends NewsArticle> chunk) {
        for (NewsArticle article : chunk.getItems()) {
            try {
                NewsCollectionEvent event = NewsCollectionEvent.builder()
                        .url(article.getUrl())
                        .source(article.getSource())
                        .collectedDate(article.getCollectedDate())
                        .build();
                
                Message<NewsCollectionEvent> message = MessageBuilder
                        .withPayload(event)
                        .setHeader(KafkaHeaders.TOPIC, TOPIC)
                        .setHeader(MEDIA_HEADER, article.getSource())
                        .build();
                
                kafkaTemplate.send(message);
                article.markAsPublished();
                log.debug("Kafka 발행 성공: {}", article.getUrl());
            } catch (Exception e) {
                log.error("Kafka 발행 실패: {}", article.getUrl(), e);
            }
        }

        repository.saveAll(chunk.getItems());
        log.info("뉴스 {} 건 Kafka 발행 완료", chunk.size());
    }
}
