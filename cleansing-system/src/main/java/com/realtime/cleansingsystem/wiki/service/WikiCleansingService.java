package com.realtime.cleansingsystem.wiki.service;

import com.realtime.cleansingsystem.common.event.WikiCleansingEvent;
import com.realtime.cleansingsystem.common.event.WikiCollectionEvent;
import com.realtime.cleansingsystem.wiki.cleaner.WikiTextCleaner;
import com.realtime.cleansingsystem.wiki.domain.CleansedWiki;
import com.realtime.cleansingsystem.wiki.domain.WikiPage;
import com.realtime.cleansingsystem.wiki.repository.WikiPageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WikiCleansingService {

    private final WikiPageRepository wikiPageRepository;
    private final WikiTextCleaner wikiTextCleaner;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Qualifier("cleansingMongoTemplate")
    private final MongoTemplate cleansingMongoTemplate;

    @Value("${kafka.topics.output.wiki}")
    private String outputTopic;

    public void process(WikiCollectionEvent event) {
        try {
            WikiPage page = wikiPageRepository.findByTitle(event.getTitle())
                    .orElseThrow(() -> new IllegalArgumentException("위키 데이터를 찾을 수 없습니다: " + event.getTitle()));

            log.info("위키 데이터 조회 완료: {}", page.getTitle());

            String cleanedText = wikiTextCleaner.clean(page.getText());

            CleansedWiki cleansedWiki = CleansedWiki.from(page, cleanedText);

            cleansingMongoTemplate.save(cleansedWiki);
            log.info("정제된 위키 데이터 저장 완료: {}", cleansedWiki.getId());

            WikiCleansingEvent cleansingEvent = WikiCleansingEvent.builder()
                    .id(cleansedWiki.getId())
                    .cleansedDate(cleansedWiki.getCleansedDate())
                    .build();

            kafkaTemplate.send(outputTopic, cleansingEvent);
            log.info("위키 정제 완료 이벤트 발행: {}", cleansedWiki.getId());

        } catch (Exception e) {
            log.error("위키 데이터 정제 실패: {}", event.getTitle(), e);
            throw e;
        }
    }
}
