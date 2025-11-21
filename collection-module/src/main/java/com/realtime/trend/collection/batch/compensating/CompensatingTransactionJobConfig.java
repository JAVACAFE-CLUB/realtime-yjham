package com.realtime.trend.collection.batch.compensating;

import com.realtime.trend.collection.domain.News;
import com.realtime.trend.collection.domain.YouTubeVideo;
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
 * 보상 트랜잭션 Batch Job 설정
 * PENDING 상태인 데이터를 Kafka로 재발행
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class CompensatingTransactionJobConfig {

    private static final String JOB_NAME = "compensatingTransactionJob";
    private static final String NEWS_STEP_NAME = "newsCompensatingStep";
    private static final String YOUTUBE_STEP_NAME = "youtubeCompensatingStep";
    private static final int CHUNK_SIZE = 10;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final NewsCompensatingReader newsCompensatingReader;
    private final NewsCompensatingWriter newsCompensatingWriter;

    private final YouTubeCompensatingReader youTubeCompensatingReader;
    private final YouTubeCompensatingWriter youTubeCompensatingWriter;

    @Bean
    public Job compensatingTransactionJob() {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(newsCompensatingStep())
                .next(youtubeCompensatingStep())
                .build();
    }

    @Bean
    public Step newsCompensatingStep() {
        return new StepBuilder(NEWS_STEP_NAME, jobRepository)
                .<News, News>chunk(CHUNK_SIZE, transactionManager)
                .reader(newsCompensatingReader)
                .writer(newsCompensatingWriter)
                .build();
    }

    @Bean
    public Step youtubeCompensatingStep() {
        return new StepBuilder(YOUTUBE_STEP_NAME, jobRepository)
                .<YouTubeVideo, YouTubeVideo>chunk(CHUNK_SIZE, transactionManager)
                .reader(youTubeCompensatingReader)
                .writer(youTubeCompensatingWriter)
                .build();
    }
}
