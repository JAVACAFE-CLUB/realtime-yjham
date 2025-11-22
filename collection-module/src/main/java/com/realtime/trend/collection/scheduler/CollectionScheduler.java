package com.realtime.trend.collection.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 데이터 수집 스케줄러
 * 뉴스: 20분마다
 * YouTube: 10분마다
 * 보상 트랜잭션: 1시간마다
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollectionScheduler {

    private final JobLauncher jobLauncher;
    private final Job newsCollectionJob;
    private final Job youtubeCollectionJob;
    private final Job newsCompensatingJob;
    private final Job youtubeCompensatingJob;

    /**
     * 뉴스 수집 Job 실행 (20분마다)
     */
    @Scheduled(cron = "0 */20 * * * *")
    public void runNewsCollection() {
        try {
            log.info("뉴스 수집 Job 시작");
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("timestamp", LocalDateTime.now())
                    .toJobParameters();

            jobLauncher.run(newsCollectionJob, jobParameters);
            log.info("뉴스 수집 Job 완료");

        } catch (Exception e) {
            log.error("뉴스 수집 Job 실행 실패", e);
        }
    }

    /**
     * YouTube 수집 Job 실행 (10분마다)
     */
    @Scheduled(cron = "0 */10 * * * *")
    public void runYoutubeCollection() {
        try {
            log.info("YouTube 수집 Job 시작");
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("timestamp", LocalDateTime.now())
                    .toJobParameters();

            jobLauncher.run(youtubeCollectionJob, jobParameters);
            log.info("YouTube 수집 Job 완료");

        } catch (Exception e) {
            log.error("YouTube 수집 Job 실행 실패", e);
        }
    }

    /**
     * 뉴스 보상 트랜잭션 Job 실행 (1시간마다)
     * PENDING 상태인 뉴스 데이터를 Kafka로 재발행
     */
    @Scheduled(cron = "0 0 * * * *")
    public void runNewsCompensatingTransaction() {
        try {
            log.info("뉴스 보상 트랜잭션 Job 시작");
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("timestamp", LocalDateTime.now())
                    .toJobParameters();

            jobLauncher.run(newsCompensatingJob, jobParameters);
            log.info("뉴스 보상 트랜잭션 Job 완료");

        } catch (Exception e) {
            log.error("뉴스 보상 트랜잭션 Job 실행 실패", e);
        }
    }

    /**
     * YouTube 보상 트랜잭션 Job 실행 (1시간마다)
     * PENDING 상태인 YouTube 데이터를 Kafka로 재발행
     */
    @Scheduled(cron = "0 5 * * * *")
    public void runYoutubeCompensatingTransaction() {
        try {
            log.info("YouTube 보상 트랜잭션 Job 시작");
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("timestamp", LocalDateTime.now())
                    .toJobParameters();

            jobLauncher.run(youtubeCompensatingJob, jobParameters);
            log.info("YouTube 보상 트랜잭션 Job 완료");

        } catch (Exception e) {
            log.error("YouTube 보상 트랜잭션 Job 실행 실패", e);
        }
    }
}
