package com.realtime.collectionsystem.wiki.batch;

import com.realtime.collectionsystem.wiki.domain.WikiPage;
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
public class WikiCollectionJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final WikiPageReader wikiPageReader;
    private final WikiPageProcessor wikiPageProcessor;
    private final WikiPageWriter wikiPageWriter;

    private final WikiKafkaReader wikiKafkaReader;
    private final WikiKafkaWriter wikiKafkaWriter;

    @Bean
    public Job wikiCollectionJob() {
        return new JobBuilder("wikiCollectionJob", jobRepository)
                .start(wikiCollectStep())
                .next(wikiPublishStep())
                .build();
    }

    @Bean
    public Step wikiCollectStep() {
        return new StepBuilder("wikiCollectStep", jobRepository)
                .<WikiPage, WikiPage>chunk(100, transactionManager)
                .reader(wikiPageReader)
                .processor(wikiPageProcessor)
                .writer(wikiPageWriter)
                .build();
    }

    @Bean
    public Step wikiPublishStep() {
        return new StepBuilder("wikiPublishStep", jobRepository)
                .<WikiPage, WikiPage>chunk(100, transactionManager)
                .reader(wikiKafkaReader)
                .writer(wikiKafkaWriter)
                .build();
    }
}
