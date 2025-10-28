package com.realtime.cleansingsystem.news.service;

import com.realtime.cleansingsystem.common.event.NewsCollectionEvent;
import com.realtime.cleansingsystem.news.cleaner.NewsTextCleaner;
import com.realtime.cleansingsystem.news.domain.CleansedNews;
import com.realtime.cleansingsystem.news.domain.NewsArticle;
import com.realtime.cleansingsystem.news.repository.NewsArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("NewsCleansingService 테스트")
@ExtendWith(MockitoExtension.class)
class NewsCleansingServiceTest {

    @Mock
    private NewsArticleRepository newsArticleRepository;

    @Mock
    private NewsTextCleaner newsTextCleaner;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private MongoTemplate cleansingMongoTemplate;

    @InjectMocks
    private NewsCleansingService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "outputTopic", "news-cleanse-topic");
    }

    @Test
    @DisplayName("뉴스 데이터 정제 성공")
    void processSuccess() {
        // given
        String url = "https://www.khan.co.kr/article/123";
        NewsCollectionEvent event = NewsCollectionEvent.builder()
                .url(url)
                .source("khan")
                .collectedDate(LocalDateTime.now())
                .build();

        NewsArticle article = NewsArticle.builder()
                .id("article123")
                .source("khan")
                .title("뉴스 제목")
                .text("원본  텍스트   내용")
                .url(url)
                .category("경제")
                .createdDate(LocalDateTime.now())
                .collectedDate(LocalDateTime.now())
                .build();

        String cleanedText = "원본 텍스트 내용";

        when(newsArticleRepository.findByUrl(url)).thenReturn(Optional.of(article));
        when(newsTextCleaner.clean(article.getText())).thenReturn(cleanedText);
        when(cleansingMongoTemplate.save(any(CleansedNews.class))).thenAnswer(i -> i.getArgument(0));

        // when
        service.process(event);

        // then
        verify(newsArticleRepository).findByUrl(url);
        verify(newsTextCleaner).clean(article.getText());

        ArgumentCaptor<CleansedNews> cleansedNewsCaptor = ArgumentCaptor.forClass(CleansedNews.class);
        verify(cleansingMongoTemplate).save(cleansedNewsCaptor.capture());

        CleansedNews savedNews = cleansedNewsCaptor.getValue();
        assertThat(savedNews.getId()).isEqualTo(article.getId());
        assertThat(savedNews.getCleanedText()).isEqualTo(cleanedText);
        assertThat(savedNews.getSource()).isEqualTo(article.getSource());

        verify(kafkaTemplate).send(eq("news-cleanse-topic"), any());
    }

    @Test
    @DisplayName("원본 데이터 없을 시 예외 발생")
    void processFailWhenArticleNotFound() {
        // given
        String url = "https://www.khan.co.kr/article/999";
        NewsCollectionEvent event = NewsCollectionEvent.builder()
                .url(url)
                .source("khan")
                .collectedDate(LocalDateTime.now())
                .build();

        when(newsArticleRepository.findByUrl(url)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.process(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("뉴스 데이터를 찾을 수 없습니다");

        verify(newsArticleRepository).findByUrl(url);
        verify(newsTextCleaner, never()).clean(any());
        verify(cleansingMongoTemplate, never()).save(any());
        verify(kafkaTemplate, never()).send(any(), any());
    }
}
