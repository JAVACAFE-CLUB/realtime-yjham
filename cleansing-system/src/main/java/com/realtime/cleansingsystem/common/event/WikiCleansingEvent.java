package com.realtime.cleansingsystem.common.event;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 위키피디아 정제 완료 이벤트
 */
@Getter
@NoArgsConstructor
public class WikiCleansingEvent extends DataCleansingEvent {

    @Builder
    public WikiCleansingEvent(String id, LocalDateTime cleansedDate) {
        super(DataCollectionEvent.DataType.WIKI, id, cleansedDate);
    }
}
