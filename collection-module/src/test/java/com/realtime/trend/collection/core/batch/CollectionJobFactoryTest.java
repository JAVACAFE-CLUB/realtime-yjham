package com.realtime.trend.collection.core.batch;

import com.realtime.trend.collection.core.config.BatchProperties;
import com.realtime.trend.collection.core.config.CompensatingProperties;
import com.realtime.trend.collection.core.domain.Publishable;
import com.realtime.trend.collection.core.domain.PublishStatus;
import com.realtime.trend.collection.core.messaging.GenericDataPublisher;
import com.realtime.trend.collection.core.metrics.CollectionMetrics;
import com.realtime.trend.collection.core.source.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@DisplayName("CollectionJobFactory 단위 테스트")
@ExtendWith(MockitoExtension.class)
class CollectionJobFactoryTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private GenericDataPublisher publisher;

    @Mock
    private CollectionMetrics metrics;

    @Mock
    private CompensatingProperties compensatingProperties;

    @Mock
    private BatchProperties batchProperties;

    @Mock
    private DataSource<TestEntity> dataSource;

    private CollectionJobFactory jobFactory;

    @BeforeEach
    void setUp() {
        jobFactory = new CollectionJobFactory(
                jobRepository, transactionManager, publisher, metrics, compensatingProperties, batchProperties);
        
        lenient().when(dataSource.getSourceName()).thenReturn("test");
        lenient().when(compensatingProperties.getMaxRetryCount()).thenReturn(5);
        lenient().when(batchProperties.getChunkSize()).thenReturn(10);
        lenient().when(batchProperties.getSkipLimit()).thenReturn(10);
        lenient().when(batchProperties.getRetryLimit()).thenReturn(3);
    }

    @Nested
    @DisplayName("createCollectionJob() 메서드")
    class CreateCollectionJobMethod {

        @Test
        @DisplayName("수집 Job을 생성해야 한다")
        @SuppressWarnings("unchecked")
        void createCollectionJob_shouldCreateJob() {
            // given
            ItemReader<TestEntity> reader = mock(ItemReader.class);
            doReturn(reader).when(dataSource).createCollectionReader();
            doReturn(null).when(dataSource).createProcessor();

            // when
            Job job = jobFactory.createCollectionJob(dataSource);

            // then
            assertThat(job).isNotNull();
            assertThat(job.getName()).isEqualTo("testCollectionJob");
        }

        @Test
        @DisplayName("Processor가 있으면 포함된 Step을 생성해야 한다")
        void createCollectionJob_withProcessor_shouldIncludeProcessor() {
            // given
            ItemReader<Object> reader = mock(ItemReader.class);
            ItemProcessor<Object, TestEntity> processor = mock(ItemProcessor.class);
            
            doReturn(reader).when(dataSource).createCollectionReader();
            doReturn(processor).when(dataSource).createProcessor();

            // when
            Job job = jobFactory.createCollectionJob(dataSource);

            // then
            assertThat(job).isNotNull();
            verify(dataSource).createProcessor();
        }

        @Test
        @DisplayName("Processor가 null이면 Reader-Writer만으로 Step을 생성해야 한다")
        void createCollectionJob_withoutProcessor_shouldCreateSimpleStep() {
            // given
            ItemReader<TestEntity> reader = mock(ItemReader.class);
            doReturn(reader).when(dataSource).createCollectionReader();
            doReturn(null).when(dataSource).createProcessor();

            // when
            Job job = jobFactory.createCollectionJob(dataSource);

            // then
            assertThat(job).isNotNull();
        }
    }

    @Nested
    @DisplayName("createCompensatingJob() 메서드")
    class CreateCompensatingJobMethod {

        @Test
        @DisplayName("보상 트랜잭션 Job을 생성해야 한다")
        void createCompensatingJob_shouldCreateJob() {
            // given
            ItemReader<TestEntity> reader = mock(ItemReader.class);
            when(dataSource.createCompensatingReader()).thenReturn(reader);

            // when
            Job job = jobFactory.createCompensatingJob(dataSource);

            // then
            assertThat(job).isNotNull();
            assertThat(job.getName()).isEqualTo("testCompensatingJob");
        }

        @Test
        @DisplayName("CompensatingProperties의 maxRetryCount를 사용해야 한다")
        void createCompensatingJob_shouldUseConfiguredMaxRetryCount() {
            // given
            ItemReader<TestEntity> reader = mock(ItemReader.class);
            when(dataSource.createCompensatingReader()).thenReturn(reader);
            when(compensatingProperties.getMaxRetryCount()).thenReturn(10);

            // when
            Job job = jobFactory.createCompensatingJob(dataSource);

            // then
            assertThat(job).isNotNull();
            verify(compensatingProperties).getMaxRetryCount();
        }
    }

    @Nested
    @DisplayName("Job 이름 규칙")
    class JobNamingConvention {

        @Test
        @DisplayName("수집 Job 이름은 '{sourceName}CollectionJob' 형식이어야 한다")
        void collectionJobName_shouldFollowNamingConvention() {
            // given
            when(dataSource.getSourceName()).thenReturn("news");
            when(dataSource.createCollectionReader()).thenReturn(mock(ItemReader.class));
            when(dataSource.createProcessor()).thenReturn(null);

            // when
            Job job = jobFactory.createCollectionJob(dataSource);

            // then
            assertThat(job.getName()).isEqualTo("newsCollectionJob");
        }

        @Test
        @DisplayName("보상 Job 이름은 '{sourceName}CompensatingJob' 형식이어야 한다")
        void compensatingJobName_shouldFollowNamingConvention() {
            // given
            when(dataSource.getSourceName()).thenReturn("youtube");
            when(dataSource.createCompensatingReader()).thenReturn(mock(ItemReader.class));

            // when
            Job job = jobFactory.createCompensatingJob(dataSource);

            // then
            assertThat(job.getName()).isEqualTo("youtubeCompensatingJob");
        }
    }

    /**
     * 테스트용 엔티티
     */
    static class TestEntity implements Publishable<TestEntity> {
        private final String id;
        private final PublishStatus publishStatus;

        TestEntity(String id, PublishStatus publishStatus) {
            this.id = id;
            this.publishStatus = publishStatus;
        }

        @Override
        public String getIdentifier() {
            return id;
        }

        @Override
        public PublishStatus getPublishStatus() {
            return publishStatus;
        }

        @Override
        public LocalDateTime getCollectedAt() {
            return LocalDateTime.now();
        }

        @Override
        public TestEntity markAsPublished() {
            return new TestEntity(id, PublishStatus.PUBLISHED);
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
            return new TestEntity(id, PublishStatus.FAILED);
        }
    }
}
