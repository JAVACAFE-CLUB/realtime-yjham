package com.realtime.cleansingsystem.common.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 데이터 수집 이벤트 베이스 클래스
 * collection-system으로부터 수신하는 이벤트
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
