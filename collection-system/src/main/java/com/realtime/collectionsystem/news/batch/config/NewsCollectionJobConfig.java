package com.realtime.collectionsystem.news.batch.config;

import com.realtime.collectionsystem.news.batch.dto.RssFeedItem;
import com.realtime.collectionsystem.news.batch.processor.NewsArticleProcessor;
import com.realtime.collectionsystem.news.batch.reader.NewsKafkaReader;
import com.realtime.collectionsystem.news.batch.reader.RssFeedReader;
import com.realtime.collectionsystem.news.batch.writer.NewsArticleWriter;
import com.realtime.collectionsystem.news.batch.writer.NewsKafkaWriter;
import com.realtime.collectionsystem.news.domain.NewsArticle;
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
public class NewsCollectionJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final RssFeedReader rssFeedReader;
    private final NewsArticleProcessor newsArticleProcessor;
    private final NewsArticleWriter newsArticleWriter;

    private final NewsKafkaReader newsKafkaReader;
    private final NewsKafkaWriter newsKafkaWriter;

    @Bean
    public Job newsCollectionJob() {
        return new JobBuilder("newsCollectionJob", jobRepository)
                .start(newsCollectStep())
                .next(newsPublishStep())
                .build();
    }

    @Bean
    public Step newsCollectStep() {
        return new StepBuilder("newsCollectStep", jobRepository)
                .<RssFeedItem, NewsArticle>chunk(100, transactionManager)
                .reader(rssFeedReader)
                .processor(newsArticleProcessor)
                .writer(newsArticleWriter)
                .build();
    }

    @Bean
    public Step newsPublishStep() {
        return new StepBuilder("newsPublishStep", jobRepository)
                .<NewsArticle, NewsArticle>chunk(100, transactionManager)
                .reader(newsKafkaReader)
                .writer(newsKafkaWriter)
                .build();
    }
}
