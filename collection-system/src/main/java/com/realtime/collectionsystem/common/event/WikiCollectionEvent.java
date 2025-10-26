package com.realtime.collectionsystem.common.event;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 위키피디아 수집 이벤트
 * MongoDB에서 조회를 위한 title 정보만 포함
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WikiCollectionEvent extends DataCollectionEvent {
    
    private String title;
    
    @Builder
    public WikiCollectionEvent(String title, LocalDateTime collectedDate) {
        super(DataType.WIKI, collectedDate);
        this.title = title;
    }
}
