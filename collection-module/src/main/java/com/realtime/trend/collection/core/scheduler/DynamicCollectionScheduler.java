package com.realtime.trend.collection.core.scheduler;

import com.realtime.trend.collection.core.batch.CollectionJobFactory;
import com.realtime.trend.collection.core.metrics.CollectionMetrics;
import com.realtime.trend.collection.core.domain.Publishable;
import com.realtime.trend.collection.core.source.DataSource;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicCollectionScheduler {

    private final List<DataSource<? extends Publishable>> dataSources;
    private final CollectionJobFactory jobFactory;
    private final JobLauncher jobLauncher;
    private final TaskScheduler taskScheduler;
    private final CollectionMetrics metrics;

    private final Map<String, Job> collectionJobs = new HashMap<>();
    private final Map<String, Job> compensatingJobs = new HashMap<>();
    
    // 동시 실행 제어를 위한 Lock
    private final Map<String, ReentrantLock> jobLocks = new ConcurrentHashMap<>();

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
        
        // Job별 Lock 초기화
        jobLocks.put(sourceName + "-collection", new ReentrantLock());
        jobLocks.put(sourceName + "-compensating", new ReentrantLock());

        // 수집 Job 스케줄링
        String collectionCron = dataSource.getCollectionCron();
        if (collectionCron != null && !collectionCron.isEmpty()) {
            taskScheduler.schedule(
                    () -> executeJobWithLock(collectionJob, sourceName, "collection"),
                    new CronTrigger(collectionCron)
            );
            log.info("[{}] 수집 Job 스케줄 등록: {}", sourceName, collectionCron);
        }

        // 보상 트랜잭션 Job 스케줄링
        String compensatingCron = dataSource.getCompensatingCron();
        if (compensatingCron != null && !compensatingCron.isEmpty()) {
            taskScheduler.schedule(
                    () -> executeJobWithLock(compensatingJob, sourceName, "compensating"),
                    new CronTrigger(compensatingCron)
            );
            log.info("[{}] 보상 트랜잭션 Job 스케줄 등록: {}", sourceName, compensatingCron);
        }

        log.info("[{}] 데이터 소스 등록 완료", sourceName);
    }

    /**
     * Lock을 사용하여 동시 실행 방지
     */
    private void executeJobWithLock(Job job, String sourceName, String jobType) {
        String lockKey = sourceName + "-" + jobType;
        ReentrantLock lock = jobLocks.get(lockKey);
        
        if (lock == null) {
            log.error("[{}] Lock을 찾을 수 없음: {}", sourceName, lockKey);
            return;
        }
        
        // tryLock으로 이미 실행 중이면 스킵
        if (!lock.tryLock()) {
            log.warn("[{}] {} Job이 이미 실행 중 - 스킵", sourceName, jobType);
            metrics.incrementJobSkipped(sourceName, jobType);
            return;
        }
        
        try {
            long startTime = System.currentTimeMillis();
            executeJob(job, sourceName + " " + jobType);
            long duration = System.currentTimeMillis() - startTime;
            metrics.recordJobDuration(sourceName, jobType, duration);
        } finally {
            lock.unlock();
        }
    }

    private void executeJob(Job job, String jobDescription) {
        try {
            log.info("{} Job 시작", jobDescription);
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLocalDateTime("timestamp", LocalDateTime.now())
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(job, jobParameters);
            log.info("{} Job 완료 - 상태: {}", jobDescription, execution.getStatus());

        } catch (JobExecutionAlreadyRunningException e) {
            log.warn("{} Job이 이미 실행 중", jobDescription);
        } catch (JobRestartException e) {
            log.error("{} Job 재시작 실패", jobDescription, e);
        } catch (JobInstanceAlreadyCompleteException e) {
            log.info("{} Job이 이미 완료됨 (동일 파라미터)", jobDescription);
        } catch (JobParametersInvalidException e) {
            log.error("{} Job 파라미터 오류", jobDescription, e);
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
            executeJobWithLock(job, sourceName, "collection");
        } else {
            log.warn("존재하지 않는 데이터 소스: {}", sourceName);
            throw new IllegalArgumentException("존재하지 않는 데이터 소스: " + sourceName);
        }
    }

    /**
     * 수동으로 보상 트랜잭션 Job 실행 (API에서 호출용)
     */
    public void runCompensatingJob(String sourceName) {
        Job job = compensatingJobs.get(sourceName);
        if (job != null) {
            executeJobWithLock(job, sourceName, "compensating");
        } else {
            log.warn("존재하지 않는 데이터 소스: {}", sourceName);
            throw new IllegalArgumentException("존재하지 않는 데이터 소스: " + sourceName);
        }
    }

    /**
     * 등록된 데이터 소스 목록 반환
     */
    public List<String> getRegisteredSources() {
        return List.copyOf(collectionJobs.keySet());
    }
    
    /**
     * 특정 Job이 현재 실행 중인지 확인
     */
    public boolean isJobRunning(String sourceName, String jobType) {
        String lockKey = sourceName + "-" + jobType;
        ReentrantLock lock = jobLocks.get(lockKey);
        return lock != null && lock.isLocked();
    }
}
