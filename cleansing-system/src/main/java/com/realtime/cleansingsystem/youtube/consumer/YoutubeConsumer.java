package com.realtime.cleansingsystem.youtube.consumer;

import com.realtime.cleansingsystem.common.event.YoutubeCollectionEvent;
import com.realtime.cleansingsystem.youtube.service.YoutubeCleansingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class YoutubeConsumer {

    private final YoutubeCleansingService youtubeCleansingService;

    @KafkaListener(
            topics = "${kafka.topics.input.youtube}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "youtubeKafkaListenerContainerFactory"
    )
    public void consume(YoutubeCollectionEvent event) {
        log.info("유튜브 수집 이벤트 수신: {}", event.getVideoId());
        youtubeCleansingService.process(event);
    }
}
