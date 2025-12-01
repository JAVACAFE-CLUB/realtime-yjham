package com.realtime.trend.collection.core.batch;

import com.realtime.trend.collection.core.domain.Publishable;
import com.realtime.trend.collection.core.messaging.GenericDataPublisher;
import com.realtime.trend.collection.core.metrics.CollectionMetrics;
import com.realtime.trend.collection.core.source.DataSource;
import com.realtime.trend.collection.core.config.BatchProperties;
import com.realtime.trend.collection.core.config.CompensatingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemWriter;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectionJobFactory {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final GenericDataPublisher publisher;
    private final CollectionMetrics metrics;
    private final CompensatingProperties compensatingProperties;
    private final BatchProperties batchProperties;

    /**
     * 수집 Job 생성
     */
    public <T extends Publishable<T>> Job createCollectionJob(DataSource<T> dataSource) {
        String jobName = dataSource.getSourceName() + "CollectionJob";

        Step step = createCollectionStep(dataSource);

        Job job = new JobBuilder(jobName, jobRepository)
                .listener(createJobListener(dataSource.getSourceName()))
                .start(step)
                .build();

        log.info("수집 Job 생성: {}", jobName);
        return job;
    }

    /**
     * 보상 트랜잭션 Job 생성
     */
    public <T extends Publishable<T>> Job createCompensatingJob(DataSource<T> dataSource) {
        String jobName = dataSource.getSourceName() + "CompensatingJob";

        Step step = createCompensatingStep(dataSource);

        Job job = new JobBuilder(jobName, jobRepository)
                .listener(createJobListener(dataSource.getSourceName()))
                .start(step)
                .build();

        log.info("보상 트랜잭션 Job 생성: {}", jobName);
        return job;
    }

    @SuppressWarnings("unchecked")
    private <T extends Publishable<T>> Step createCollectionStep(DataSource<T> dataSource) {
        String stepName = dataSource.getSourceName() + "CollectionStep";

        ItemReader<?> reader = dataSource.createCollectionReader();
        ItemProcessor<?, T> processor = dataSource.createProcessor();
        GenericItemWriter<T> writer = new GenericItemWriter<>(dataSource, publisher, metrics);

        SimpleStepBuilder<Object, T> stepBuilder;

        if (processor != null) {
            stepBuilder = new StepBuilder(stepName, jobRepository)
                    .<Object, T>chunk(batchProperties.getChunkSize(), transactionManager)
                    .reader((ItemReader<Object>) reader)
                    .processor((ItemProcessor<Object, T>) processor)
                    .writer(writer);
        } else {
            stepBuilder = new StepBuilder(stepName, jobRepository)
                    .<Object, T>chunk(batchProperties.getChunkSize(), transactionManager)
                    .reader((ItemReader<Object>) reader)
                    .writer(writer);
        }

        // ItemStream 등록 (Reader가 ItemStream을 구현한 경우)
        if (reader instanceof ItemStream) {
            stepBuilder.stream((ItemStream) reader);
        }

        // Fault Tolerance 정책 추가
        return stepBuilder
                .faultTolerant()
                .skipLimit(batchProperties.getSkipLimit())
                .skip(Exception.class)
                .noSkip(IllegalArgumentException.class)  // 잘못된 인자는 스킵하지 않음
                .retryLimit(batchProperties.getRetryLimit())
                .retry(TransientDataAccessException.class)
                .listener(createStepListener(dataSource.getSourceName()))
                .build();
    }

    private <T extends Publishable<T>> Step createCompensatingStep(DataSource<T> dataSource) {
        String stepName = dataSource.getSourceName() + "CompensatingStep";

        ItemReader<T> reader = dataSource.createCompensatingReader();
        GenericCompensatingWriter<T> writer = new GenericCompensatingWriter<>(
                dataSource, publisher, metrics, compensatingProperties.getMaxRetryCount());

        SimpleStepBuilder<T, T> stepBuilder = new StepBuilder(stepName, jobRepository)
                .<T, T>chunk(batchProperties.getChunkSize(), transactionManager)
                .reader(reader)
                .writer(writer);

        // ItemStream 등록
        if (reader instanceof ItemStream) {
            stepBuilder.stream((ItemStream) reader);
        }

        // Fault Tolerance 정책 추가
        return stepBuilder
                .faultTolerant()
                .skipLimit(batchProperties.getSkipLimit())
                .skip(Exception.class)
                .retryLimit(batchProperties.getRetryLimit())
                .retry(TransientDataAccessException.class)
                .listener(createStepListener(dataSource.getSourceName()))
                .build();
    }

    /**
     * Job 실행 리스너 생성
     */
    private JobExecutionListener createJobListener(String sourceName) {
        return new JobExecutionListener() {
            @Override
            public void beforeJob(JobExecution jobExecution) {
                log.info("[{}] Job 시작 - JobId: {}, Parameters: {}",
                        sourceName,
                        jobExecution.getJobId(),
                        jobExecution.getJobParameters());
            }

            @Override
            public void afterJob(JobExecution jobExecution) {
                BatchStatus status = jobExecution.getStatus();
                long duration = jobExecution.getEndTime() != null && jobExecution.getStartTime() != null
                        ? java.time.Duration.between(jobExecution.getStartTime(), jobExecution.getEndTime()).toMillis()
                        : 0;

                if (status == BatchStatus.COMPLETED) {
                    log.info("[{}] Job 완료 - JobId: {}, 소요시간: {}ms",
                            sourceName, jobExecution.getJobId(), duration);
                } else if (status == BatchStatus.FAILED) {
                    log.error("[{}] Job 실패 - JobId: {}, 소요시간: {}ms, 예외: {}",
                            sourceName, jobExecution.getJobId(), duration,
                            jobExecution.getAllFailureExceptions());
                } else {
                    log.warn("[{}] Job 종료 - JobId: {}, 상태: {}, 소요시간: {}ms",
                            sourceName, jobExecution.getJobId(), status, duration);
                }
            }
        };
    }

    /**
     * Step 실행 리스너 생성 (Skip/Retry 모니터링)
     */
    private StepExecutionListener createStepListener(String sourceName) {
        return new StepExecutionListener() {
            @Override
            public void beforeStep(StepExecution stepExecution) {
                log.debug("[{}] Step 시작: {}", sourceName, stepExecution.getStepName());
            }

            @Override
            public ExitStatus afterStep(StepExecution stepExecution) {
                long readCount = stepExecution.getReadCount();
                long writeCount = stepExecution.getWriteCount();
                long skipCount = stepExecution.getSkipCount();
                long rollbackCount = stepExecution.getRollbackCount();

                log.info("[{}] Step 완료: {} - 읽기: {}, 쓰기: {}, 스킵: {}, 롤백: {}",
                        sourceName,
                        stepExecution.getStepName(),
                        readCount, writeCount, skipCount, rollbackCount);

                if (skipCount > 0) {
                    log.warn("[{}] Step에서 {}개 아이템 스킵됨", sourceName, skipCount);
                }

                return stepExecution.getExitStatus();
            }
        };
    }
}
