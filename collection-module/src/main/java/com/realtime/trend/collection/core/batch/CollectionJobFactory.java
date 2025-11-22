package com.realtime.trend.collection.core.batch;

import com.realtime.trend.collection.core.domain.Publishable;
import com.realtime.trend.collection.core.messaging.GenericDataPublisher;
import com.realtime.trend.collection.core.source.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 데이터 소스별 Batch Job 자동 생성 팩토리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollectionJobFactory {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final GenericDataPublisher publisher;

    private static final int CHUNK_SIZE = 10;

    /**
     * 수집 Job 생성
     */
    public <T extends Publishable> Job createCollectionJob(DataSource<T> dataSource) {
        String jobName = dataSource.getSourceName() + "CollectionJob";

        Step step = createCollectionStep(dataSource);

        Job job = new JobBuilder(jobName, jobRepository)
                .start(step)
                .build();

        log.info("수집 Job 생성: {}", jobName);
        return job;
    }

    /**
     * 보상 트랜잭션 Job 생성
     */
    public <T extends Publishable> Job createCompensatingJob(DataSource<T> dataSource) {
        String jobName = dataSource.getSourceName() + "CompensatingJob";

        Step step = createCompensatingStep(dataSource);

        Job job = new JobBuilder(jobName, jobRepository)
                .start(step)
                .build();

        log.info("보상 트랜잭션 Job 생성: {}", jobName);
        return job;
    }

    @SuppressWarnings("unchecked")
    private <T extends Publishable> Step createCollectionStep(DataSource<T> dataSource) {
        String stepName = dataSource.getSourceName() + "CollectionStep";

        ItemReader<?> reader = dataSource.createCollectionReader();
        ItemProcessor<?, T> processor = dataSource.createProcessor();
        GenericItemWriter<T> writer = new GenericItemWriter<>(dataSource, publisher);

        if (processor != null) {
            return new StepBuilder(stepName, jobRepository)
                    .<Object, T>chunk(CHUNK_SIZE, transactionManager)
                    .reader((ItemReader<Object>) reader)
                    .processor((ItemProcessor<Object, T>) processor)
                    .writer(writer)
                    .build();
        } else {
            return new StepBuilder(stepName, jobRepository)
                    .<T, T>chunk(CHUNK_SIZE, transactionManager)
                    .reader((ItemReader<T>) reader)
                    .writer(writer)
                    .build();
        }
    }

    private <T extends Publishable> Step createCompensatingStep(DataSource<T> dataSource) {
        String stepName = dataSource.getSourceName() + "CompensatingStep";

        ItemReader<T> reader = dataSource.createCompensatingReader();
        GenericCompensatingWriter<T> writer = new GenericCompensatingWriter<>(dataSource, publisher);

        return new StepBuilder(stepName, jobRepository)
                .<T, T>chunk(CHUNK_SIZE, transactionManager)
                .reader(reader)
                .writer(writer)
                .build();
    }
}
