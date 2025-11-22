package com.realtime.trend.collection.youtube.batch;

import com.realtime.trend.collection.youtube.domain.YouTubeVideo;
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
 * YouTube 보상 트랜잭션 Batch Job 설정
 * PENDING 상태인 YouTube 데이터를 Kafka로 재발행
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class YouTubeCompensatingJobConfig {

    private static final String JOB_NAME = "youtubeCompensatingJob";
    private static final String STEP_NAME = "youtubeCompensatingStep";
    private static final int CHUNK_SIZE = 10;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final YouTubeCompensatingReader youTubeCompensatingReader;
    private final YouTubeCompensatingWriter youTubeCompensatingWriter;

    @Bean
    public Job youtubeCompensatingJob() {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(youtubeCompensatingStep())
                .build();
    }

    @Bean
    public Step youtubeCompensatingStep() {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<YouTubeVideo, YouTubeVideo>chunk(CHUNK_SIZE, transactionManager)
                .reader(youTubeCompensatingReader)
                .writer(youTubeCompensatingWriter)
                .build();
    }
}
