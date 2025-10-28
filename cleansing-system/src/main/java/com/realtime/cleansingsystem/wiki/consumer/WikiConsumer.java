package com.realtime.cleansingsystem.wiki.consumer;

import com.realtime.cleansingsystem.common.event.WikiCollectionEvent;
import com.realtime.cleansingsystem.wiki.service.WikiCleansingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WikiConsumer {

    private final WikiCleansingService wikiCleansingService;

    @KafkaListener(
            topics = "${kafka.topics.input.wiki}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "wikiKafkaListenerContainerFactory"
    )
    public void consume(WikiCollectionEvent event) {
        log.info("위키 수집 이벤트 수신: {}", event.getTitle());
        wikiCleansingService.process(event);
    }
}
