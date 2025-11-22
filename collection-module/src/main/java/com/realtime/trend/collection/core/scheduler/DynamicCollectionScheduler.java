package com.realtime.trend.collection.core.scheduler;

import com.realtime.trend.collection.core.batch.CollectionJobFactory;
import com.realtime.trend.collection.core.domain.Publishable;
import com.realtime.trend.collection.core.source.DataSource;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 동적 스케줄러
 * 등록된 모든 DataSource에 대해 자동으로 Job 스케줄링
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicCollectionScheduler {

    private final List<DataSource<? extends Publishable>> dataSources;
    private final CollectionJobFactory jobFactory;
    private final JobLauncher jobLauncher;
    private final TaskScheduler taskScheduler;

    private final Map<String, Job> collectionJobs = new HashMap<>();
    private final Map<String, Job> compensatingJobs = new HashMap<>();

    @PostConstruct
    public void initialize() {
        for (DataSource<? extends Publishable> dataSource : dataSources) {
            if (!dataSource.isEnabled()) {
                log.info("[{}] 데이터 소스 비활성화됨 - 스킵", dataSource.getSourceName());
                continue;
            }

            registerDataSource(dataSource);
        }
    }

    private void registerDataSource(DataSource<? extends Publishable> dataSource) {
        String sourceName = dataSource.getSourceName();
        log.info("[{}] 데이터 소스 등록 시작", sourceName);

        // Job 생성
        Job collectionJob = jobFactory.createCollectionJob(dataSource);
        Job compensatingJob = jobFactory.createCompensatingJob(dataSource);

        collectionJobs.put(sourceName, collectionJob);
        compensatingJobs.put(sourceName, compensatingJob);

        // 수집 Job 스케줄링
        String collectionCron = dataSource.getCollectionCron();
        if (collectionCron != null && !collectionCron.isEmpty()) {
            taskScheduler.schedule(
                    () -> executeJob(collectionJob, sourceName + " 수집"),
                    new CronTrigger(collectionCron)
            );
            log.info("[{}] 수집 Job 스케줄 등록: {}", sourceName, collectionCron);
        }

        // 보상 트랜잭션 Job 스케줄링
        String compensatingCron = dataSource.getCompensatingCron();
        if (compensatingCron != null && !compensatingCron.isEmpty()) {
            taskScheduler.schedule(
                    () -> executeJob(compensatingJob, sourceName + " 보상 트랜잭션"),
                    new CronTrigger(compensatingCron)
            );
            log.info("[{}] 보상 트랜잭션 Job 스케줄 등록: {}", sourceName, compensatingCron);
        }

        log.info("[{}] 데이터 소스 등록 완료", sourceName);
    }

    private void executeJob(Job job, String jobDescription) {
        try {
            log.info("{} Job 시작", jobDescription);
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("timestamp", LocalDateTime.now())
                    .toJobParameters();

            jobLauncher.run(job, jobParameters);
            log.info("{} Job 완료", jobDescription);

        } catch (Exception e) {
            log.error("{} Job 실행 실패", jobDescription, e);
        }
    }

    /**
     * 수동으로 수집 Job 실행 (API에서 호출용)
     */
    public void runCollectionJob(String sourceName) {
        Job job = collectionJobs.get(sourceName);
        if (job != null) {
            executeJob(job, sourceName + " 수집 (수동)");
        } else {
            log.warn("존재하지 않는 데이터 소스: {}", sourceName);
        }
    }

    /**
     * 수동으로 보상 트랜잭션 Job 실행 (API에서 호출용)
     */
    public void runCompensatingJob(String sourceName) {
        Job job = compensatingJobs.get(sourceName);
        if (job != null) {
            executeJob(job, sourceName + " 보상 트랜잭션 (수동)");
        } else {
            log.warn("존재하지 않는 데이터 소스: {}", sourceName);
        }
    }

    /**
     * 등록된 데이터 소스 목록 반환
     */
    public List<String> getRegisteredSources() {
        return List.copyOf(collectionJobs.keySet());
    }
}
