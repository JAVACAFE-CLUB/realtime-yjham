package com.realtime.cleansingsystem.youtube.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 정제된 유튜브 데이터
 * cleansing database에 저장
 */
@Document(collection = "cleansed_youtube")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CleansedYoutube {

    @Id
    private String id;  // 원본 YoutubeVideo의 id와 동일하게 유지

    @Indexed
    private String videoId;

    private String title;

    private String cleanedDescription;  // URL/이모지 제거된 설명

    private List<String> tags;

    private String channelTitle;

    private String categoryId;

    private LocalDateTime createdDate;

    private LocalDateTime collectedDate;

    @Indexed
    private LocalDateTime cleansedDate;

    public static CleansedYoutube from(YoutubeVideo video, String cleanedDescription) {
        return CleansedYoutube.builder()
                .id(video.getId())
                .videoId(video.getVideoId())
                .title(video.getTitle())
                .cleanedDescription(cleanedDescription)
                .tags(video.getTags())
                .channelTitle(video.getChannelTitle())
                .categoryId(video.getCategoryId())
                .createdDate(video.getCreatedDate())
                .collectedDate(video.getCollectedDate())
                .cleansedDate(LocalDateTime.now())
                .build();
    }
}
