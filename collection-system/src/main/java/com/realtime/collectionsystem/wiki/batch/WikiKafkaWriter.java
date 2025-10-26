package com.realtime.collectionsystem.wiki.batch;

import com.realtime.collectionsystem.common.event.WikiCollectionEvent;
import com.realtime.collectionsystem.wiki.domain.WikiPage;
import com.realtime.collectionsystem.wiki.repository.WikiPageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WikiKafkaWriter implements ItemWriter<WikiPage> {

    private static final String TOPIC = "wiki-collect-topic";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final WikiPageRepository repository;

    @Override
    public void write(Chunk<? extends WikiPage> chunk) {
        for (WikiPage page : chunk.getItems()) {
            try {
                WikiCollectionEvent event = WikiCollectionEvent.builder()
                        .title(page.getTitle())
                        .collectedDate(page.getCollectedDate())
                        .build();
                
                kafkaTemplate.send(TOPIC, event);
                page.markAsPublished();
                log.debug("Kafka 발행 성공: {}", page.getTitle());
            } catch (Exception e) {
                log.error("Kafka 발행 실패: {}", page.getTitle(), e);
            }
        }

        repository.saveAll(chunk.getItems());
        log.info("위키피디아 {} 건 Kafka 발행 완료", chunk.size());
    }
}
