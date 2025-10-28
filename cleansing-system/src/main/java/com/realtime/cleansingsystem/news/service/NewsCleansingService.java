package com.realtime.cleansingsystem.news.service;

import com.realtime.cleansingsystem.common.event.NewsCleansingEvent;
import com.realtime.cleansingsystem.common.event.NewsCollectionEvent;
import com.realtime.cleansingsystem.news.cleaner.NewsTextCleaner;
import com.realtime.cleansingsystem.news.domain.CleansedNews;
import com.realtime.cleansingsystem.news.domain.NewsArticle;
import com.realtime.cleansingsystem.news.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsCleansingService {

    private final NewsArticleRepository newsArticleRepository;
    private final NewsTextCleaner newsTextCleaner;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Qualifier("cleansingMongoTemplate")
    private final MongoTemplate cleansingMongoTemplate;

    @Value("${kafka.topics.output.news}")
    private String outputTopic;

    /**
     * 뉴스 데이터 정제 처리
     */
    public void process(NewsCollectionEvent event) {
        try {
            // 1. 원본 데이터 조회
            NewsArticle article = newsArticleRepository.findByUrl(event.getUrl())
                    .orElseThrow(() -> new IllegalArgumentException("뉴스 데이터를 찾을 수 없습니다: " + event.getUrl()));

            log.info("뉴스 데이터 조회 완료: {}", article.getUrl());

            // 2. 텍스트 정제
            String cleanedText = newsTextCleaner.clean(article.getText());

            // 3. 정제 데이터 객체 생성
            CleansedNews cleansedNews = CleansedNews.from(article, cleanedText);

            // 4. cleansing database에 저장
            cleansingMongoTemplate.save(cleansedNews);
            log.info("정제된 뉴스 데이터 저장 완료: {}", cleansedNews.getId());

            // 5. Kafka 정제 완료 이벤트 발행
            NewsCleansingEvent cleansingEvent = NewsCleansingEvent.builder()
                    .id(cleansedNews.getId())
                    .cleansedDate(cleansedNews.getCleansedDate())
                    .build();

            kafkaTemplate.send(outputTopic, cleansingEvent);
            log.info("뉴스 정제 완료 이벤트 발행: {}", cleansedNews.getId());

        } catch (Exception e) {
            log.error("뉴스 데이터 정제 실패: {}", event.getUrl(), e);
            throw e;
        }
    }
}
