package com.realtime.collectionsystem.news.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsCollectionScheduler {

    private final JobLauncher jobLauncher;
    private final Job newsCollectionJob;

    @EventListener(ApplicationReadyEvent.class)
    public void runOnStartup() {
        log.info("뉴스 수집 배치 시작 (초기 실행)");
        runJob();
    }

    @Scheduled(cron = "0 0 * * * *")
    public void runScheduled() {
        log.info("뉴스 수집 배치 시작 (스케줄 실행)");
        runJob();
    }

    private void runJob() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(newsCollectionJob, params);
            log.info("뉴스 수집 배치 완료");
        } catch (Exception e) {
            log.error("뉴스 수집 배치 실패", e);
        }
    }
}
