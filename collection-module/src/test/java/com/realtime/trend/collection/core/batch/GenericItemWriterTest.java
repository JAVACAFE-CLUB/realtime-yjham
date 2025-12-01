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

@DisplayName("GenericItemWriter 단위 테스트")
@ExtendWith(MockitoExtension.class)
class GenericItemWriterTest {

    @Mock
    private DataSource<TestEntity> dataSource;

    @Mock
    private GenericDataPublisher publisher;

    @Mock
    private CollectionMetrics metrics;

    private GenericItemWriter<TestEntity> writer;

    @BeforeEach
    void setUp() {
        writer = new GenericItemWriter<>(dataSource, publisher, metrics);
        when(dataSource.getSourceName()).thenReturn("test");
        when(dataSource.getTopicName()).thenReturn("test-topic");
    }

    @Nested
    @DisplayName("write() 메서드")
    class WriteMethod {

        @Test
        @DisplayName("저장 및 발행 성공 시 PUBLISHED 상태로 변경해야 한다")
        void write_withSuccessfulPublish_shouldMarkAsPublished() throws Exception {
            // given
            TestEntity item = new TestEntity("id-1", PublishStatus.PENDING);
            TestEntity savedItem = new TestEntity("id-1", PublishStatus.PENDING);
            TestEntity publishedItem = new TestEntity("id-1", PublishStatus.PUBLISHED);

            when(dataSource.save(any())).thenReturn(savedItem).thenReturn(publishedItem);
            when(publisher.publishSync(anyString(), anyString(), any())).thenReturn(true);

            Chunk<TestEntity> chunk = new Chunk<>(item);

            // when
            writer.write(chunk);

            // then
            verify(dataSource, times(2)).save(any()); // PENDING 저장 + PUBLISHED 저장
            verify(publisher).publishSync(eq("test-topic"), eq("id-1"), any());
            verify(metrics).incrementCollected("test");
            verify(metrics).incrementPublished("test");
            verify(metrics).recordPublishTime(eq("test"), anyLong());
        }

        @Test
        @DisplayName("발행 실패 시 PENDING 상태를 유지해야 한다")
        void write_withFailedPublish_shouldKeepPending() throws Exception {
            // given
            TestEntity item = new TestEntity("id-1", PublishStatus.PENDING);
            TestEntity savedItem = new TestEntity("id-1", PublishStatus.PENDING);

            when(dataSource.save(any())).thenReturn(savedItem);
            when(publisher.publishSync(anyString(), anyString(), any())).thenReturn(false);

            Chunk<TestEntity> chunk = new Chunk<>(item);

            // when
            writer.write(chunk);

            // then
            verify(dataSource, times(1)).save(any()); // PENDING 저장만
            verify(metrics).incrementCollected("test");
            verify(metrics).incrementFailed("test");
            verify(metrics, never()).incrementPublished("test");
        }

        @Test
        @DisplayName("저장 실패 시 해당 아이템만 스킵하고 계속 진행해야 한다")
        void write_withSaveFailed_shouldSkipAndContinue() throws Exception {
            // given
            TestEntity item1 = new TestEntity("id-1", PublishStatus.PENDING);
            TestEntity item2 = new TestEntity("id-2", PublishStatus.PENDING);
            TestEntity savedItem2 = new TestEntity("id-2", PublishStatus.PENDING);
            TestEntity publishedItem2 = new TestEntity("id-2", PublishStatus.PUBLISHED);

            when(dataSource.save(item1)).thenThrow(new RuntimeException("Save failed"));
            when(dataSource.save(item2)).thenReturn(savedItem2);
            when(dataSource.save(argThat(e -> e != null && e.getPublishStatus() == PublishStatus.PUBLISHED)))
                    .thenReturn(publishedItem2);
            when(publisher.publishSync(anyString(), anyString(), any())).thenReturn(true);

            Chunk<TestEntity> chunk = new Chunk<>(item1, item2);

            // when
            writer.write(chunk);

            // then
            verify(metrics).incrementSaveFailed("test");
            verify(metrics).incrementCollected("test"); // item2만 성공
            verify(metrics).incrementPublished("test"); // item2만 발행
        }

        @Test
        @DisplayName("여러 아이템을 순차적으로 처리해야 한다")
        void write_withMultipleItems_shouldProcessSequentially() throws Exception {
            // given
            TestEntity item1 = new TestEntity("id-1", PublishStatus.PENDING);
            TestEntity item2 = new TestEntity("id-2", PublishStatus.PENDING);

            when(dataSource.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(publisher.publishSync(anyString(), anyString(), any())).thenReturn(true);

            Chunk<TestEntity> chunk = new Chunk<>(item1, item2);

            // when
            writer.write(chunk);

            // then
            verify(dataSource, times(4)).save(any()); // 2 * (PENDING + PUBLISHED)
            verify(publisher, times(2)).publishSync(anyString(), anyString(), any());
            verify(metrics, times(2)).incrementCollected("test");
            verify(metrics, times(2)).incrementPublished("test");
        }

        @Test
        @DisplayName("발행 성공 후 상태 변경 실패 시 로그만 남기고 계속 진행해야 한다")
        void write_withStatusUpdateFailed_shouldLogAndContinue() throws Exception {
            // given
            TestEntity item = new TestEntity("id-1", PublishStatus.PENDING);
            TestEntity savedItem = new TestEntity("id-1", PublishStatus.PENDING);

            when(dataSource.save(item)).thenReturn(savedItem);
            when(dataSource.save(argThat(e -> e != null && e.getPublishStatus() == PublishStatus.PUBLISHED)))
                    .thenThrow(new RuntimeException("Status update failed"));
            when(publisher.publishSync(anyString(), anyString(), any())).thenReturn(true);

            Chunk<TestEntity> chunk = new Chunk<>(item);

            // when
            writer.write(chunk);

            // then
            verify(metrics).incrementCollected("test");
            verify(metrics).incrementPublished("test"); // 발행은 성공했으므로 카운트
        }
    }

    /**
     * 테스트용 엔티티
     */
    static class TestEntity implements Publishable<TestEntity> {
        private final String id;
        private final PublishStatus publishStatus;
        private final int retryCount;

        TestEntity(String id, PublishStatus publishStatus) {
            this(id, publishStatus, 0);
        }

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
