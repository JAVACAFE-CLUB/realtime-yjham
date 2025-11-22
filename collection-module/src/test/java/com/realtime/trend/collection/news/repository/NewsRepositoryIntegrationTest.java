package com.realtime.trend.collection.news.repository;

import com.realtime.trend.collection.CollectionApplication;
import com.realtime.trend.collection.core.domain.PublishStatus;
import com.realtime.trend.collection.news.domain.News;
import com.realtime.trend.test.config.TestcontainersConfig;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NewsRepository 통합 테스트")
@SpringBootTest(classes = CollectionApplication.class)
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestcontainersConfig.MongoInitializer.class)
class NewsRepositoryIntegrationTest {

    @Autowired
    private NewsRepository newsRepository;

    @BeforeEach
    void setUp() {
        newsRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        newsRepository.deleteAll();
    }

    @Nested
    @DisplayName("기본 CRUD 테스트")
    class BasicCrudTests {

        @Test
        @DisplayName("뉴스를 저장하고 조회할 수 있어야 한다")
        void save_andFindById_shouldWork() {
            // given
            News news = createTestNews("https://example.com/news/1");

            // when
            News saved = newsRepository.save(news);
            Optional<News> found = newsRepository.findById(saved.getId());

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getTitle()).isEqualTo(news.getTitle());
            assertThat(found.get().getContent()).isEqualTo(news.getContent());
        }

        @Test
        @DisplayName("뉴스를 삭제할 수 있어야 한다")
        void delete_shouldWork() {
            // given
            News news = newsRepository.save(createTestNews("https://example.com/news/2"));

            // when
            newsRepository.deleteById(news.getId());

            // then
            assertThat(newsRepository.findById(news.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("URL 기반 조회 테스트")
    class UrlBasedQueryTests {

        @Test
        @DisplayName("URL로 뉴스를 조회할 수 있어야 한다")
        void findByUrl_shouldReturnNews() {
            // given
            String url = "https://example.com/news/unique";
            News news = newsRepository.save(createTestNews(url));

            // when
            Optional<News> found = newsRepository.findByUrl(url);

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(news.getId());
        }

        @Test
        @DisplayName("존재하지 않는 URL은 빈 Optional을 반환해야 한다")
        void findByUrl_withNonExistentUrl_shouldReturnEmpty() {
            // when
            Optional<News> found = newsRepository.findByUrl("https://notexist.com");

            // then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("URL 존재 여부를 확인할 수 있어야 한다")
        void existsByUrl_shouldReturnCorrectResult() {
            // given
            String url = "https://example.com/news/exists";
            newsRepository.save(createTestNews(url));

            // when & then
            assertThat(newsRepository.existsByUrl(url)).isTrue();
            assertThat(newsRepository.existsByUrl("https://notexist.com")).isFalse();
        }
    }

    @Nested
    @DisplayName("발행 상태 기반 조회 테스트")
    class PublishStatusQueryTests {

        @Test
        @DisplayName("발행 상태로 뉴스를 조회할 수 있어야 한다")
        void findByPublishStatus_shouldReturnMatchingNews() {
            // given
            News pending1 = newsRepository.save(createTestNewsWithStatus(
                    "https://example.com/1", PublishStatus.PENDING));
            News pending2 = newsRepository.save(createTestNewsWithStatus(
                    "https://example.com/2", PublishStatus.PENDING));
            News published = newsRepository.save(createTestNewsWithStatus(
                    "https://example.com/3", PublishStatus.PUBLISHED));

            // when
            List<News> pendingNews = newsRepository.findByPublishStatus(PublishStatus.PENDING);
            List<News> publishedNews = newsRepository.findByPublishStatus(PublishStatus.PUBLISHED);

            // then
            assertThat(pendingNews).hasSize(2);
            assertThat(publishedNews).hasSize(1);
            assertThat(publishedNews.get(0).getUrl()).isEqualTo(published.getUrl());
        }

        @Test
        @DisplayName("발행 상태와 수집 시각으로 뉴스를 조회할 수 있어야 한다")
        void findByPublishStatusAndCollectedAtBefore_shouldWork() {
            // given
            LocalDateTime now = LocalDateTime.now();
            News oldPending = createTestNewsWithStatusAndTime(
                    "https://example.com/old", PublishStatus.PENDING, now.minusHours(2));
            News recentPending = createTestNewsWithStatusAndTime(
                    "https://example.com/recent", PublishStatus.PENDING, now.minusMinutes(30));

            newsRepository.save(oldPending);
            newsRepository.save(recentPending);

            // when
            List<News> result = newsRepository.findByPublishStatusAndCollectedAtBefore(
                    PublishStatus.PENDING, now.minusHours(1));

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUrl()).isEqualTo(oldPending.getUrl());
        }
    }

    @Nested
    @DisplayName("기간 및 언론사 기반 조회 테스트")
    class RangeAndPublisherQueryTests {

        @Test
        @DisplayName("특정 기간의 뉴스를 조회할 수 있어야 한다")
        void findByCollectedAtBetween_shouldReturnNewsInRange() {
            // given
            LocalDateTime now = LocalDateTime.now();
            News yesterday = createTestNewsWithStatusAndTime(
                    "https://example.com/yesterday", PublishStatus.PENDING, now.minusDays(1));
            News today = createTestNewsWithStatusAndTime(
                    "https://example.com/today", PublishStatus.PENDING, now);
            News lastWeek = createTestNewsWithStatusAndTime(
                    "https://example.com/lastweek", PublishStatus.PENDING, now.minusWeeks(1));

            newsRepository.saveAll(List.of(yesterday, today, lastWeek));

            // when
            List<News> result = newsRepository.findByCollectedAtBetween(
                    now.minusDays(2), now.plusDays(1));

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("언론사별 뉴스를 조회할 수 있어야 한다")
        void findByPublisher_shouldReturnMatchingNews() {
            // given
            News khan = createTestNewsWithPublisher("https://khan.com/1", "경향신문");
            News kmib = createTestNewsWithPublisher("https://kmib.com/1", "국민일보");

            newsRepository.saveAll(List.of(khan, kmib));

            // when
            List<News> khanNews = newsRepository.findByPublisher("경향신문");
            List<News> kmibNews = newsRepository.findByPublisher("국민일보");

            // then
            assertThat(khanNews).hasSize(1);
            assertThat(kmibNews).hasSize(1);
        }
    }

    // 테스트 데이터 생성 헬퍼 메서드
    private News createTestNews(String url) {
        return News.builder()
                .url(url)
                .title("테스트 뉴스 제목")
                .content("테스트 뉴스 본문입니다.")
                .publishedAt(LocalDateTime.now())
                .publisher("테스트신문")
                .author("테스트 기자")
                .category("테스트")
                .collectedAt(LocalDateTime.now())
                .publishStatus(PublishStatus.PENDING)
                .build();
    }

    private News createTestNewsWithStatus(String url, PublishStatus status) {
        return News.builder()
                .url(url)
                .title("테스트 뉴스")
                .content("테스트 본문")
                .publishedAt(LocalDateTime.now())
                .publisher("테스트신문")
                .collectedAt(LocalDateTime.now())
                .publishStatus(status)
                .build();
    }

    private News createTestNewsWithStatusAndTime(String url, PublishStatus status, LocalDateTime collectedAt) {
        return News.builder()
                .url(url)
                .title("테스트 뉴스")
                .content("테스트 본문")
                .publishedAt(LocalDateTime.now())
                .publisher("테스트신문")
                .collectedAt(collectedAt)
                .publishStatus(status)
                .build();
    }

    private News createTestNewsWithPublisher(String url, String publisher) {
        return News.builder()
                .url(url)
                .title("테스트 뉴스")
                .content("테스트 본문")
                .publishedAt(LocalDateTime.now())
                .publisher(publisher)
                .collectedAt(LocalDateTime.now())
                .publishStatus(PublishStatus.PENDING)
                .build();
    }
}
