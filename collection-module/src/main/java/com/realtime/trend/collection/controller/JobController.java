package com.realtime.trend.collection.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 배치 Job 수동 실행 컨트롤러 (테스트용)
 */
@Slf4j
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobLauncher jobLauncher;
    private final Job newsCollectionJob;
    private final Job youtubeCollectionJob;
    private final Job compensatingTransactionJob;

    /**
     * 뉴스 수집 Job 즉시 실행
     */
    @PostMapping("/news")
    public Map<String, Object> runNewsCollection() {
        Map<String, Object> response = new HashMap<>();
        try {
            log.info("뉴스 수집 Job 수동 실행 시작");
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("timestamp", LocalDateTime.now())
                    .toJobParameters();

            jobLauncher.run(newsCollectionJob, jobParameters);

            response.put("success", true);
            response.put("message", "뉴스 수집 Job이 실행되었습니다.");
            log.info("뉴스 수집 Job 수동 실행 완료");

        } catch (Exception e) {
            log.error("뉴스 수집 Job 실행 실패", e);
            response.put("success", false);
            response.put("message", "뉴스 수집 Job 실행 실패: " + e.getMessage());
        }
        return response;
    }

    /**
     * YouTube 수집 Job 즉시 실행
     */
    @PostMapping("/youtube")
    public Map<String, Object> runYoutubeCollection() {
        Map<String, Object> response = new HashMap<>();
        try {
            log.info("YouTube 수집 Job 수동 실행 시작");
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("timestamp", LocalDateTime.now())
                    .toJobParameters();

            jobLauncher.run(youtubeCollectionJob, jobParameters);

            response.put("success", true);
            response.put("message", "YouTube 수집 Job이 실행되었습니다.");
            log.info("YouTube 수집 Job 수동 실행 완료");

        } catch (Exception e) {
            log.error("YouTube 수집 Job 실행 실패", e);
            response.put("success", false);
            response.put("message", "YouTube 수집 Job 실행 실패: " + e.getMessage());
        }
        return response;
    }

    /**
     * 보상 트랜잭션 Job 즉시 실행
     */
    @PostMapping("/compensating")
    public Map<String, Object> runCompensatingTransaction() {
        Map<String, Object> response = new HashMap<>();
        try {
            log.info("보상 트랜잭션 Job 수동 실행 시작");
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("timestamp", LocalDateTime.now())
                    .toJobParameters();

            jobLauncher.run(compensatingTransactionJob, jobParameters);

            response.put("success", true);
            response.put("message", "보상 트랜잭션 Job이 실행되었습니다.");
            log.info("보상 트랜잭션 Job 수동 실행 완료");

        } catch (Exception e) {
            log.error("보상 트랜잭션 Job 실행 실패", e);
            response.put("success", false);
            response.put("message", "보상 트랜잭션 Job 실행 실패: " + e.getMessage());
        }
        return response;
    }
}
