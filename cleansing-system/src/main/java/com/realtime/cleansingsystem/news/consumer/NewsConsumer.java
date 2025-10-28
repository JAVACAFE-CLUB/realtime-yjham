package com.realtime.cleansingsystem.news.consumer;

import com.realtime.cleansingsystem.common.event.NewsCollectionEvent;
import com.realtime.cleansingsystem.news.service.NewsCleansingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsConsumer {

    private final NewsCleansingService newsCleansingService;

    @KafkaListener(
            topics = "${kafka.topics.input.news}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "newsKafkaListenerContainerFactory"
    )
    public void consume(NewsCollectionEvent event) {
        log.info("뉴스 수집 이벤트 수신: {}", event.getUrl());
        newsCleansingService.process(event);
    }
}
