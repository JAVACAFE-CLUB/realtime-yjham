package com.realtime.cleansingsystem.youtube.service;

import com.realtime.cleansingsystem.common.event.YoutubeCleansingEvent;
import com.realtime.cleansingsystem.common.event.YoutubeCollectionEvent;
import com.realtime.cleansingsystem.youtube.cleaner.YoutubeTextCleaner;
import com.realtime.cleansingsystem.youtube.domain.CleansedYoutube;
import com.realtime.cleansingsystem.youtube.domain.YoutubeVideo;
import com.realtime.cleansingsystem.youtube.repository.YoutubeVideoRepository;
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
public class YoutubeCleansingService {

    private final YoutubeVideoRepository youtubeVideoRepository;
    private final YoutubeTextCleaner youtubeTextCleaner;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Qualifier("cleansingMongoTemplate")
    private final MongoTemplate cleansingMongoTemplate;

    @Value("${kafka.topics.output.youtube}")
    private String outputTopic;

    public void process(YoutubeCollectionEvent event) {
        try {
            YoutubeVideo video = youtubeVideoRepository.findByVideoId(event.getVideoId())
                    .orElseThrow(() -> new IllegalArgumentException("유튜브 데이터를 찾을 수 없습니다: " + event.getVideoId()));

            log.info("유튜브 데이터 조회 완료: {}", video.getVideoId());

            String cleanedDescription = youtubeTextCleaner.clean(video.getDescription());

            CleansedYoutube cleansedYoutube = CleansedYoutube.from(video, cleanedDescription);

            cleansingMongoTemplate.save(cleansedYoutube);
            log.info("정제된 유튜브 데이터 저장 완료: {}", cleansedYoutube.getId());

            YoutubeCleansingEvent cleansingEvent = YoutubeCleansingEvent.builder()
                    .id(cleansedYoutube.getId())
                    .cleansedDate(cleansedYoutube.getCleansedDate())
                    .build();

            kafkaTemplate.send(outputTopic, cleansingEvent);
            log.info("유튜브 정제 완료 이벤트 발행: {}", cleansedYoutube.getId());

        } catch (Exception e) {
            log.error("유튜브 데이터 정제 실패: {}", event.getVideoId(), e);
            throw e;
        }
    }
}
