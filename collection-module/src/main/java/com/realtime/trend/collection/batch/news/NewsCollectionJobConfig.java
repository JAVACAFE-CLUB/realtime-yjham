package com.realtime.trend.collection.batch.news;

import com.realtime.trend.collection.domain.News;
import com.realtime.trend.collection.service.crawler.RssItem;
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
 * 뉴스 수집 Batch Job 설정
 * RSS 수집 → HTML 크롤링 → MongoDB 저장 및 Kafka 발행
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class NewsCollectionJobConfig {

    private static final String JOB_NAME = "newsCollectionJob";
    private static final String STEP_NAME = "newsCollectionStep";
    private static final int CHUNK_SIZE = 10;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final RssItemReader rssItemReader;
    private final NewsItemProcessor newsItemProcessor;
    private final NewsItemWriter newsItemWriter;

    @Bean
    public Job newsCollectionJob() {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(newsCollectionStep())
                .build();
    }

    @Bean
    public Step newsCollectionStep() {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<RssItem, News>chunk(CHUNK_SIZE, transactionManager)
                .reader(rssItemReader)
                .processor(newsItemProcessor)
                .writer(newsItemWriter)
                .build();
    }
}
