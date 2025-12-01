package com.realtime.trend.collection.core.scheduler;

import com.realtime.trend.collection.core.batch.CollectionJobFactory;
import com.realtime.trend.collection.core.metrics.CollectionMetrics;
import com.realtime.trend.collection.core.domain.Publishable;
import com.realtime.trend.collection.core.domain.PublishStatus;
import com.realtime.trend.collection.core.source.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.TaskScheduler;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("DynamicCollectionScheduler 단위 테스트")
@ExtendWith(MockitoExtension.class)
class DynamicCollectionSchedulerTest {

    @Mock
    private CollectionJobFactory jobFactory;

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private CollectionMetrics metrics;

    @Mock
    private DataSource<TestEntity> newsDataSource;

    @Mock
    private DataSource<TestEntity> youtubeDataSource;

    @Mock
    private Job newsCollectionJob;

    @Mock
    private Job newsCompensatingJob;

    @Mock
    private Job youtubeCollectionJob;

    @Mock
    private Job youtubeCompensatingJob;

    private DynamicCollectionScheduler scheduler;

    @BeforeEach
    void setUp() {
        // News DataSource 설정
        lenient().when(newsDataSource.getSourceName()).thenReturn("news");
        lenient().when(newsDataSource.isEnabled()).thenReturn(true);
        lenient().when(newsDataSource.getCollectionCron()).thenReturn("0 */10 * * * *");
        lenient().when(newsDataSource.getCompensatingCron()).thenReturn("0 */30 * * * *");

        // YouTube DataSource 설정
        lenient().when(youtubeDataSource.getSourceName()).thenReturn("youtube");
        lenient().when(youtubeDataSource.isEnabled()).thenReturn(true);
        lenient().when(youtubeDataSource.getCollectionCron()).thenReturn("0 */15 * * * *");
        lenient().when(youtubeDataSource.getCompensatingCron()).thenReturn("0 */30 * * * *");

        // JobFactory Mock 설정
        lenient().when(jobFactory.createCollectionJob(newsDataSource)).thenReturn(newsCollectionJob);
        lenient().when(jobFactory.createCompensatingJob(newsDataSource)).thenReturn(newsCompensatingJob);
        lenient().when(jobFactory.createCollectionJob(youtubeDataSource)).thenReturn(youtubeCollectionJob);
        lenient().when(jobFactory.createCompensatingJob(youtubeDataSource)).thenReturn(youtubeCompensatingJob);

        // 스케줄러 생성
        @SuppressWarnings("unchecked")
        List<DataSource<? extends Publishable>> dataSources = List.of(
                (DataSource<? extends Publishable>) newsDataSource,
                (DataSource<? extends Publishable>) youtubeDataSource
        );
        scheduler = new DynamicCollectionScheduler(dataSources, jobFactory, jobLauncher, taskScheduler, metrics);
    }

    @Nested
    @DisplayName("initialize() 메서드")
    class InitializeMethod {

        @Test
        @DisplayName("활성화된 데이터 소스에 대해 Job을 생성하고 스케줄링해야 한다")
        void initialize_withEnabledDataSources_shouldCreateAndScheduleJobs() {
            // when
            scheduler.initialize();

            // then
            verify(jobFactory).createCollectionJob(newsDataSource);
            verify(jobFactory).createCompensatingJob(newsDataSource);
            verify(jobFactory).createCollectionJob(youtubeDataSource);
            verify(jobFactory).createCompensatingJob(youtubeDataSource);

            // 스케줄 등록 확인 (4개: news 수집/보상, youtube 수집/보상)
            verify(taskScheduler, times(4)).schedule(any(Runnable.class), any(org.springframework.scheduling.support.CronTrigger.class));
        }

        @Test
        @DisplayName("비활성화된 데이터 소스는 스킵해야 한다")
        void initialize_withDisabledDataSource_shouldSkip() {
            // given
            when(newsDataSource.isEnabled()).thenReturn(false);

            // when
            scheduler.initialize();

            // then
            verify(jobFactory, never()).createCollectionJob(newsDataSource);
            verify(jobFactory).createCollectionJob(youtubeDataSource);
        }

        @Test
        @DisplayName("cron 표현식이 없으면 스케줄링하지 않아야 한다")
        void initialize_withoutCron_shouldNotSchedule() {
            // given
            when(newsDataSource.getCollectionCron()).thenReturn(null);
            when(newsDataSource.getCompensatingCron()).thenReturn("");

            // when
            scheduler.initialize();

            // then
            // news는 스케줄 안됨 (2개 감소), youtube만 스케줄됨 (2개)
            verify(taskScheduler, times(2)).schedule(any(Runnable.class), any(org.springframework.scheduling.support.CronTrigger.class));
        }
    }

    @Nested
    @DisplayName("getRegisteredSources() 메서드")
    class GetRegisteredSourcesMethod {

        @Test
        @DisplayName("등록된 데이터 소스 이름 목록을 반환해야 한다")
        void getRegisteredSources_shouldReturnSourceNames() {
            // given
            scheduler.initialize();

            // when
            List<String> sources = scheduler.getRegisteredSources();

            // then
            assertThat(sources).containsExactlyInAnyOrder("news", "youtube");
        }

        @Test
        @DisplayName("초기화 전에는 빈 목록을 반환해야 한다")
        void getRegisteredSources_beforeInitialize_shouldReturnEmptyList() {
            // when
            List<String> sources = scheduler.getRegisteredSources();

            // then
            assertThat(sources).isEmpty();
        }
    }

    @Nested
    @DisplayName("runCollectionJob() 메서드")
    class RunCollectionJobMethod {

        @Test
        @DisplayName("존재하지 않는 소스명이면 IllegalArgumentException을 던져야 한다")
        void runCollectionJob_withInvalidSourceName_shouldThrowException() {
            // given
            scheduler.initialize();

            // when & then
            assertThatThrownBy(() -> scheduler.runCollectionJob("invalid-source"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 데이터 소스");
        }

        @Test
        @DisplayName("등록된 소스명이면 Job을 실행해야 한다")
        void runCollectionJob_withValidSourceName_shouldExecuteJob() throws Exception {
            // given
            scheduler.initialize();

            // when
            scheduler.runCollectionJob("news");

            // then
            verify(jobLauncher).run(eq(newsCollectionJob), any());
        }
    }

    @Nested
    @DisplayName("runCompensatingJob() 메서드")
    class RunCompensatingJobMethod {

        @Test
        @DisplayName("존재하지 않는 소스명이면 IllegalArgumentException을 던져야 한다")
        void runCompensatingJob_withInvalidSourceName_shouldThrowException() {
            // given
            scheduler.initialize();

            // when & then
            assertThatThrownBy(() -> scheduler.runCompensatingJob("invalid-source"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 데이터 소스");
        }

        @Test
        @DisplayName("등록된 소스명이면 Job을 실행해야 한다")
        void runCompensatingJob_withValidSourceName_shouldExecuteJob() throws Exception {
            // given
            scheduler.initialize();

            // when
            scheduler.runCompensatingJob("youtube");

            // then
            verify(jobLauncher).run(eq(youtubeCompensatingJob), any());
        }
    }

    @Nested
    @DisplayName("isJobRunning() 메서드")
    class IsJobRunningMethod {

        @Test
        @DisplayName("Job이 실행 중이 아니면 false를 반환해야 한다")
        void isJobRunning_whenNotRunning_shouldReturnFalse() {
            // given
            scheduler.initialize();

            // when
            boolean running = scheduler.isJobRunning("news", "collection");

            // then
            assertThat(running).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 소스/타입이면 false를 반환해야 한다")
        void isJobRunning_withInvalidSource_shouldReturnFalse() {
            // given
            scheduler.initialize();

            // when
            boolean running = scheduler.isJobRunning("invalid", "collection");

            // then
            assertThat(running).isFalse();
        }
    }

    /**
     * 테스트용 엔티티
     */
    static class TestEntity implements Publishable<TestEntity> {
        private final String id;

        TestEntity(String id) {
            this.id = id;
        }

        @Override
        public String getIdentifier() {
            return id;
        }

        @Override
        public PublishStatus getPublishStatus() {
            return PublishStatus.PENDING;
        }

        @Override
        public LocalDateTime getCollectedAt() {
            return LocalDateTime.now();
        }

        @Override
        public TestEntity markAsPublished() {
            return this;
        }

        @Override
        public int getRetryCount() {
            return 0;
        }

        @Override
        public TestEntity incrementRetryCount() {
            return this;
        }

        @Override
        public TestEntity markAsFailed() {
            return this;
        }
    }
}
