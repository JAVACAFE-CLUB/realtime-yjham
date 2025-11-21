package com.realtime.trend.collection.repository;

import com.realtime.trend.collection.domain.News;
import com.realtime.trend.collection.domain.PublishStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NewsRepository 통합 테스트
 * 실행 전 docker-compose.infra.yml로 인프라를 먼저 시작해야 합니다.
 */
@SpringBootTest
@ActiveProfiles("test")
class NewsRepositoryTest {

    @Autowired
    private NewsRepository newsRepository;

    @AfterEach
    void tearDown() {
        newsRepository.deleteAll();
    }

    @Test
    @DisplayName("뉴스 저장 및 조회")
    void saveAndFind() {
        // given
        News news = News.builder()
                .url("https://example.com/news/1")
                .title("테스트 뉴스")
                .content("테스트 내용")
                .publisher("테스트 언론사")
                .publishedAt(LocalDateTime.now())
                .collectedAt(LocalDateTime.now())
                .publishStatus(PublishStatus.PENDING)
                .build();

        // when
        News saved = newsRepository.save(news);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUrl()).isEqualTo("https://example.com/news/1");
        assertThat(saved.getTitle()).isEqualTo("테스트 뉴스");
    }

    @Test
    @DisplayName("URL로 뉴스 조회")
    void findByUrl() {
        // given
        News news = createNews("https://example.com/news/1", "뉴스1");
        newsRepository.save(news);

        // when
        Optional<News> found = newsRepository.findByUrl("https://example.com/news/1");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("뉴스1");
    }

    @Test
    @DisplayName("URL 존재 여부 확인")
    void existsByUrl() {
        // given
        News news = createNews("https://example.com/news/1", "뉴스1");
        newsRepository.save(news);

        // when & then
        assertThat(newsRepository.existsByUrl("https://example.com/news/1")).isTrue();
        assertThat(newsRepository.existsByUrl("https://example.com/news/999")).isFalse();
    }

    @Test
    @DisplayName("발행 상태로 조회")
    void findByPublishStatus() {
        // given
        newsRepository.save(createNewsWithStatus("https://example.com/news/1", PublishStatus.PENDING));
        newsRepository.save(createNewsWithStatus("https://example.com/news/2", PublishStatus.PENDING));
        newsRepository.save(createNewsWithStatus("https://example.com/news/3", PublishStatus.PUBLISHED));

        // when
        List<News> pendingNews = newsRepository.findByPublishStatus(PublishStatus.PENDING);

        // then
        assertThat(pendingNews).hasSize(2);
    }

    @Test
    @DisplayName("발행 대기 상태이고 1시간 이전 데이터 조회")
    void findPendingNewsBeforeOneHour() {
        // given
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        LocalDateTime twoHoursAgo = LocalDateTime.now().minusHours(2);

        newsRepository.save(createNewsWithStatusAndTime(
                "https://example.com/news/1",
                PublishStatus.PENDING,
                twoHoursAgo
        ));
        newsRepository.save(createNewsWithStatusAndTime(
                "https://example.com/news/2",
                PublishStatus.PENDING,
                LocalDateTime.now()
        ));

        // when
        List<News> oldPendingNews = newsRepository
                .findByPublishStatusAndCollectedAtBefore(PublishStatus.PENDING, oneHourAgo);

        // then
        assertThat(oldPendingNews).hasSize(1);
        assertThat(oldPendingNews.get(0).getUrl()).isEqualTo("https://example.com/news/1");
    }

    private News createNews(String url, String title) {
        return News.builder()
                .url(url)
                .title(title)
                .content("테스트 내용")
                .publisher("테스트 언론사")
                .publishedAt(LocalDateTime.now())
                .collectedAt(LocalDateTime.now())
                .publishStatus(PublishStatus.PENDING)
                .build();
    }

    private News createNewsWithStatus(String url, PublishStatus status) {
        return News.builder()
                .url(url)
                .title("테스트 뉴스")
                .content("테스트 내용")
                .publisher("테스트 언론사")
                .publishedAt(LocalDateTime.now())
                .collectedAt(LocalDateTime.now())
                .publishStatus(status)
                .build();
    }

    private News createNewsWithStatusAndTime(String url, PublishStatus status, LocalDateTime collectedAt) {
        return News.builder()
                .url(url)
                .title("테스트 뉴스")
                .content("테스트 내용")
                .publisher("테스트 언론사")
                .publishedAt(LocalDateTime.now())
                .collectedAt(collectedAt)
                .publishStatus(status)
                .build();
    }
}
