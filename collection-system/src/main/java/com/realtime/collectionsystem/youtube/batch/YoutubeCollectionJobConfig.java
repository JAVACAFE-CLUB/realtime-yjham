package com.realtime.collectionsystem.youtube.batch;

import com.realtime.collectionsystem.youtube.domain.YoutubeVideo;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class YoutubeCollectionJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final YoutubeVideoReader youtubeVideoReader;
    private final YoutubeVideoProcessor youtubeVideoProcessor;
    private final YoutubeVideoWriter youtubeVideoWriter;

    private final YoutubeKafkaReader youtubeKafkaReader;
    private final YoutubeKafkaWriter youtubeKafkaWriter;

    @Bean
    public Job youtubeCollectionJob() {
        return new JobBuilder("youtubeCollectionJob", jobRepository)
                .start(youtubeCollectStep())
                .next(youtubePublishStep())
                .build();
    }

    @Bean
    public Step youtubeCollectStep() {
        return new StepBuilder("youtubeCollectStep", jobRepository)
                .<YoutubeVideo, YoutubeVideo>chunk(100, transactionManager)
                .reader(youtubeVideoReader)
                .processor(youtubeVideoProcessor)
                .writer(youtubeVideoWriter)
                .build();
    }

    @Bean
    public Step youtubePublishStep() {
        return new StepBuilder("youtubePublishStep", jobRepository)
                .<YoutubeVideo, YoutubeVideo>chunk(100, transactionManager)
                .reader(youtubeKafkaReader)
                .writer(youtubeKafkaWriter)
                .build();
    }
}
