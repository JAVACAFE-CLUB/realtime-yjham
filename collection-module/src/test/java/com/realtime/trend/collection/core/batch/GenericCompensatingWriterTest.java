package com.realtime.trend.collection.core.batch;

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
import org.springframework.batch.item.Chunk;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("GenericCompensatingWriter 단위 테스트")
@ExtendWith(MockitoExtension.class)
class GenericCompensatingWriterTest {

    @Mock
    private DataSource<TestEntity> dataSource;

    @Mock
    private GenericDataPublisher publisher;

    @Mock
    private CollectionMetrics metrics;

    private GenericCompensatingWriter<TestEntity> writer;

    private static final int MAX_RETRY_COUNT = 3;

    @BeforeEach
    void setUp() {
        writer = new GenericCompensatingWriter<>(dataSource, publisher, metrics, MAX_RETRY_COUNT);
        lenient().when(dataSource.getSourceName()).thenReturn("test");
        lenient().when(dataSource.getTopicName()).thenReturn("test-topic");
    }

    @Nested
    @DisplayName("write() 메서드")
    class WriteMethod {

        @Test
        @DisplayName("발행 성공 시 PUBLISHED 상태로 변경해야 한다")
        void write_withSuccessfulPublish_shouldMarkAsPublished() throws Exception {
            // given
            TestEntity item = new TestEntity("id-1", PublishStatus.PENDING, 0);
            when(dataSource.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(publisher.publishSync(anyString(), anyString(), any())).thenReturn(true);

            Chunk<TestEntity> chunk = new Chunk<>(item);

            // when
            writer.write(chunk);

            // then
            verify(dataSource).save(argThat(e -> e.getPublishStatus() == PublishStatus.PUBLISHED));
            verify(metrics).incrementCompensatingSuccess("test");
            verify(metrics, never()).incrementCompensatingFailed("test");
        }

        @Test
        @DisplayName("발행 실패 시 재시도 횟수를 증가시켜야 한다")
        void write_withFailedPublish_shouldIncrementRetryCount() throws Exception {
            // given
            TestEntity item = new TestEntity("id-1", PublishStatus.PENDING, 1);
            when(dataSource.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(publisher.publishSync(anyString(), anyString(), any())).thenReturn(false);

            Chunk<TestEntity> chunk = new Chunk<>(item);

            // when
            writer.write(chunk);

            // then
            verify(dataSource).save(argThat(e -> e.getRetryCount() == 2));
            verify(metrics, never()).incrementCompensatingSuccess("test");
            verify(metrics, never()).incrementCompensatingFailed("test");
        }

        @Test
        @DisplayName("최대 재시도 횟수 초과 시 FAILED 상태로 변경해야 한다")
        void write_withMaxRetryExceeded_shouldMarkAsFailed() throws Exception {
            // given
            TestEntity item = new TestEntity("id-1", PublishStatus.PENDING, MAX_RETRY_COUNT);
            when(dataSource.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Chunk<TestEntity> chunk = new Chunk<>(item);

            // when
            writer.write(chunk);

            // then
            verify(dataSource).save(argThat(e -> e.getPublishStatus() == PublishStatus.FAILED));
            verify(metrics).incrementCompensatingFailed("test");
            verify(metrics, never()).incrementCompensatingSuccess("test");
            verify(publisher, never()).publishSync(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("이미 PUBLISHED 상태인 아이템은 스킵해야 한다 (멱등성)")
        void write_withAlreadyPublished_shouldSkip() throws Exception {
            // given
            TestEntity item = new TestEntity("id-1", PublishStatus.PUBLISHED, 0);

            Chunk<TestEntity> chunk = new Chunk<>(item);

            // when
            writer.write(chunk);

            // then
            verify(dataSource, never()).save(any());
            verify(publisher, never()).publishSync(anyString(), anyString(), any());
            verify(metrics, never()).incrementCompensatingSuccess("test");
            verify(metrics, never()).incrementCompensatingFailed("test");
        }

        @Test
        @DisplayName("여러 아이템을 순차적으로 처리해야 한다")
        void write_withMultipleItems_shouldProcessSequentially() throws Exception {
            // given
            TestEntity item1 = new TestEntity("id-1", PublishStatus.PENDING, 0);
            TestEntity item2 = new TestEntity("id-2", PublishStatus.PENDING, MAX_RETRY_COUNT);
            TestEntity item3 = new TestEntity("id-3", PublishStatus.PUBLISHED, 0);

            when(dataSource.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(publisher.publishSync(anyString(), eq("id-1"), any())).thenReturn(true);

            Chunk<TestEntity> chunk = new Chunk<>(item1, item2, item3);

            // when
            writer.write(chunk);

            // then
            verify(metrics).incrementCompensatingSuccess("test"); // item1
            verify(metrics).incrementCompensatingFailed("test"); // item2
            verify(publisher, times(1)).publishSync(anyString(), anyString(), any()); // item1만
        }

        @Test
        @DisplayName("재시도 횟수가 최대값 미만이면 계속 PENDING 상태를 유지해야 한다")
        void write_withRetryCountBelowMax_shouldKeepPending() throws Exception {
            // given
            TestEntity item = new TestEntity("id-1", PublishStatus.PENDING, MAX_RETRY_COUNT - 1);
            when(dataSource.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(publisher.publishSync(anyString(), anyString(), any())).thenReturn(false);

            Chunk<TestEntity> chunk = new Chunk<>(item);

            // when
            writer.write(chunk);

            // then
            verify(dataSource).save(argThat(e -> 
                e.getPublishStatus() == PublishStatus.PENDING && 
                e.getRetryCount() == MAX_RETRY_COUNT
            ));
        }
    }

    @Nested
    @DisplayName("기본 생성자")
    class DefaultConstructor {

        @Test
        @DisplayName("기본 최대 재시도 횟수는 5여야 한다")
        void defaultMaxRetryCount_shouldBeFive() throws Exception {
            // given
            GenericCompensatingWriter<TestEntity> defaultWriter = 
                    new GenericCompensatingWriter<>(dataSource, publisher, metrics);
            TestEntity item = new TestEntity("id-1", PublishStatus.PENDING, 5);
            when(dataSource.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Chunk<TestEntity> chunk = new Chunk<>(item);

            // when
            defaultWriter.write(chunk);

            // then
            verify(dataSource).save(argThat(e -> e.getPublishStatus() == PublishStatus.FAILED));
        }
    }

    /**
     * 테스트용 엔티티
     */
    static class TestEntity implements Publishable<TestEntity> {
        private final String id;
        private final PublishStatus publishStatus;
        private final int retryCount;

        TestEntity(String id, PublishStatus publishStatus, int retryCount) {
            this.id = id;
            this.publishStatus = publishStatus;
            this.retryCount = retryCount;
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
            return new TestEntity(id, PublishStatus.PUBLISHED, retryCount);
        }

        @Override
        public int getRetryCount() {
            return retryCount;
        }

        @Override
        public TestEntity incrementRetryCount() {
            return new TestEntity(id, publishStatus, retryCount + 1);
        }

        @Override
        public TestEntity markAsFailed() {
            return new TestEntity(id, PublishStatus.FAILED, retryCount);
        }
    }
}
