package com.realtime.collectionsystem.common.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 데이터 수집 이벤트 베이스 클래스
 * MongoDB에서 조회를 위한 최소한의 식별자 정보만 포함
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public abstract class DataCollectionEvent {
    
    private DataType dataType;
    private LocalDateTime collectedDate;
    
    public enum DataType {
        NEWS, WIKI, YOUTUBE
    }
}
