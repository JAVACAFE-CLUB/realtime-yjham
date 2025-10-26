package com.realtime.collectionsystem.wiki.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WikiCollectionScheduler {

    private final JobLauncher jobLauncher;
    private final Job wikiCollectionJob;

    @EventListener(ApplicationReadyEvent.class)
    public void runOnStartup() {
        log.info("위키피디아 수집 배치 시작 (초기 실행)");
        runJob();
    }

    private void runJob() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(wikiCollectionJob, params);
            log.info("위키피디아 수집 배치 완료");
        } catch (Exception e) {
            log.error("위키피디아 수집 배치 실패", e);
        }
    }
}
