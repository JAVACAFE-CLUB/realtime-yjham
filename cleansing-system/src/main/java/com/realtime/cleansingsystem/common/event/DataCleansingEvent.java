package com.realtime.cleansingsystem.common.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 데이터 정제 완료 이벤트 베이스 클래스
 * indexing-system으로 전달하는 이벤트
 * MongoDB _id만 포함하여 indexing-system이 직접 조회하도록 함
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public abstract class DataCleansingEvent {

    private DataCollectionEvent.DataType dataType;
    private String id;  // MongoDB _id
    private LocalDateTime cleansedDate;
}
