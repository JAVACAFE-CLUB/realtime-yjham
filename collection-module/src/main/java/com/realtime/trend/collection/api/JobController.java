package com.realtime.trend.collection.api;

import com.realtime.trend.collection.core.scheduler.DynamicCollectionScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 배치 Job 수동 실행 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final DynamicCollectionScheduler scheduler;

    /**
     * 등록된 데이터 소스 목록 조회
     */
    @GetMapping("/sources")
    public Map<String, Object> getRegisteredSources() {
        Map<String, Object> response = new HashMap<>();
        List<String> sources = scheduler.getRegisteredSources();
        response.put("sources", sources);
        return response;
    }

    /**
     * 뉴스 수집 Job 즉시 실행
     */
    @PostMapping("/news")
    public Map<String, Object> runNewsCollection() {
        return runCollectionJob("news");
    }

    /**
     * YouTube 수집 Job 즉시 실행
     */
    @PostMapping("/youtube")
    public Map<String, Object> runYoutubeCollection() {
        return runCollectionJob("youtube");
    }

    /**
     * 뉴스 보상 트랜잭션 Job 즉시 실행
     */
    @PostMapping("/compensating/news")
    public Map<String, Object> runNewsCompensatingTransaction() {
        return runCompensatingJob("news");
    }

    /**
     * YouTube 보상 트랜잭션 Job 즉시 실행
     */
    @PostMapping("/compensating/youtube")
    public Map<String, Object> runYoutubeCompensatingTransaction() {
        return runCompensatingJob("youtube");
    }

    /**
     * 범용 수집 Job 실행
     */
    @PostMapping("/{sourceName}")
    public Map<String, Object> runCollectionJob(@PathVariable String sourceName) {
        Map<String, Object> response = new HashMap<>();
        try {
            log.info("{} 수집 Job 수동 실행 시작", sourceName);
            scheduler.runCollectionJob(sourceName);
            response.put("success", true);
            response.put("message", sourceName + " 수집 Job이 실행되었습니다.");
            log.info("{} 수집 Job 수동 실행 완료", sourceName);
        } catch (Exception e) {
            log.error("{} 수집 Job 실행 실패", sourceName, e);
            response.put("success", false);
            response.put("message", sourceName + " 수집 Job 실행 실패: " + e.getMessage());
        }
        return response;
    }

    /**
     * 범용 보상 트랜잭션 Job 실행
     */
    @PostMapping("/compensating/{sourceName}")
    public Map<String, Object> runCompensatingJob(@PathVariable String sourceName) {
        Map<String, Object> response = new HashMap<>();
        try {
            log.info("{} 보상 트랜잭션 Job 수동 실행 시작", sourceName);
            scheduler.runCompensatingJob(sourceName);
            response.put("success", true);
            response.put("message", sourceName + " 보상 트랜잭션 Job이 실행되었습니다.");
            log.info("{} 보상 트랜잭션 Job 수동 실행 완료", sourceName);
        } catch (Exception e) {
            log.error("{} 보상 트랜잭션 Job 실행 실패", sourceName, e);
            response.put("success", false);
            response.put("message", sourceName + " 보상 트랜잭션 Job 실행 실패: " + e.getMessage());
        }
        return response;
    }
}
