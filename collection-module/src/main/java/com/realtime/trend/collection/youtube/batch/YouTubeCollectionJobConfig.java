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
 * YouTube 수집 Batch Job 설정
 * YouTube API 호출 → MongoDB 저장 및 Kafka 발행
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class YouTubeCollectionJobConfig {

    private static final String JOB_NAME = "youtubeCollectionJob";
    private static final String STEP_NAME = "youtubeCollectionStep";
    private static final int CHUNK_SIZE = 10;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final YouTubeVideoReader youTubeVideoReader;
    private final YouTubeVideoProcessor youTubeVideoProcessor;
    private final YouTubeVideoWriter youTubeVideoWriter;

    @Bean
    public Job youtubeCollectionJob() {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(youtubeCollectionStep())
                .build();
    }

    @Bean
    public Step youtubeCollectionStep() {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<YouTubeVideo, YouTubeVideo>chunk(CHUNK_SIZE, transactionManager)
                .reader(youTubeVideoReader)
                .processor(youTubeVideoProcessor)
                .writer(youTubeVideoWriter)
                .build();
    }
}
