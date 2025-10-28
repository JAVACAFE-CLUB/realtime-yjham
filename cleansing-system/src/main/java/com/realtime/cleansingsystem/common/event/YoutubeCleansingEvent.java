package com.realtime.cleansingsystem.common.event;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 유튜브 정제 완료 이벤트
 */
@Getter
@NoArgsConstructor
public class YoutubeCleansingEvent extends DataCleansingEvent {

    @Builder
    public YoutubeCleansingEvent(String id, LocalDateTime cleansedDate) {
        super(DataCollectionEvent.DataType.YOUTUBE, id, cleansedDate);
    }
}
