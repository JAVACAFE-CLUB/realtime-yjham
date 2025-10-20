package com.realtime.collectionsystem.youtube.batch;

import com.realtime.collectionsystem.youtube.domain.YoutubeVideo;
import com.realtime.collectionsystem.youtube.repository.YoutubeVideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class YoutubeKafkaWriter implements ItemWriter<YoutubeVideo> {

    private static final String TOPIC = "youtube-collect-topic";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final YoutubeVideoRepository repository;

    @Override
    public void write(Chunk<? extends YoutubeVideo> chunk) {
        for (YoutubeVideo video : chunk.getItems()) {
            try {
                kafkaTemplate.send(TOPIC, video);
                video.markAsPublished();
                log.debug("Kafka 발행 성공: {}", video.getVideoId());
            } catch (Exception e) {
                log.error("Kafka 발행 실패: {}", video.getVideoId(), e);
            }
        }

        repository.saveAll(chunk.getItems());
        log.info("YouTube 동영상 {} 건 Kafka 발행 완료", chunk.size());
    }
}
