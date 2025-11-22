package com.realtime.trend.collection.news.batch;

import com.realtime.trend.collection.news.domain.News;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 뉴스 보상 트랜잭션 Batch Job 설정
 * PENDING 상태인 뉴스 데이터를 Kafka로 재발행
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class NewsCompensatingJobConfig {

    private static final String JOB_NAME = "newsCompensatingJob";
    private static final String STEP_NAME = "newsCompensatingStep";
    private static final int CHUNK_SIZE = 10;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final NewsCompensatingReader newsCompensatingReader;
    private final NewsCompensatingWriter newsCompensatingWriter;

    @Bean
    public Job newsCompensatingJob() {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(newsCompensatingStep())
                .build();
    }

    @Bean
    public Step newsCompensatingStep() {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<News, News>chunk(CHUNK_SIZE, transactionManager)
                .reader(newsCompensatingReader)
                .writer(newsCompensatingWriter)
                .build();
    }
}
