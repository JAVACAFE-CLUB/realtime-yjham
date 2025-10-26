package com.realtime.collectionsystem.common.event;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * YouTube 수집 이벤트
 * MongoDB에서 조회를 위한 videoId 정보만 포함
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class YoutubeCollectionEvent extends DataCollectionEvent {
    
    private String videoId;
    
    @Builder
    public YoutubeCollectionEvent(String videoId, LocalDateTime collectedDate) {
        super(DataType.YOUTUBE, collectedDate);
        this.videoId = videoId;
    }
}
