package com.realtime.cleansingsystem.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 뉴스 수집 이벤트
 * MongoDB에서 조회를 위한 url과 source 정보만 포함
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NewsCollectionEvent extends DataCollectionEvent {

    private String url;
    private String source;

    @Builder
    public NewsCollectionEvent(String url, String source, LocalDateTime collectedDate) {
        super(DataType.NEWS, collectedDate);
        this.url = url;
        this.source = source;
    }
}
