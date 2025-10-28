package com.realtime.cleansingsystem.common.event;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 뉴스 정제 완료 이벤트
 */
@Getter
@NoArgsConstructor
public class NewsCleansingEvent extends DataCleansingEvent {

    @Builder
    public NewsCleansingEvent(String id, LocalDateTime cleansedDate) {
        super(DataCollectionEvent.DataType.NEWS, id, cleansedDate);
    }
}
