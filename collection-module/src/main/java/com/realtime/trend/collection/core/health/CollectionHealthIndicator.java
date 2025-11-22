package com.realtime.trend.collection.core.health;

import com.realtime.trend.collection.core.scheduler.DynamicCollectionScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 수집 모듈 Health Indicator
 * Spring Boot Actuator /actuator/health 엔드포인트에 수집 상태 정보 제공
 */
@Component
@RequiredArgsConstructor
public class CollectionHealthIndicator implements HealthIndicator {

    private final DynamicCollectionScheduler scheduler;

    @Override
    public Health health() {
        List<String> registeredSources = scheduler.getRegisteredSources();
        
        if (registeredSources.isEmpty()) {
            return Health.down()
                    .withDetail("message", "등록된 데이터 소스가 없습니다")
                    .build();
        }

        Map<String, Object> details = new HashMap<>();
        details.put("registeredSources", registeredSources);
        details.put("sourceCount", registeredSources.size());
        
        Map<String, Object> jobStatus = new HashMap<>();
        for (String source : registeredSources) {
            Map<String, Boolean> sourceStatus = new HashMap<>();
            sourceStatus.put("collectionRunning", scheduler.isJobRunning(source, "collection"));
            sourceStatus.put("compensatingRunning", scheduler.isJobRunning(source, "compensating"));
            jobStatus.put(source, sourceStatus);
        }
        details.put("jobStatus", jobStatus);

        return Health.up()
                .withDetails(details)
                .build();
    }
}
