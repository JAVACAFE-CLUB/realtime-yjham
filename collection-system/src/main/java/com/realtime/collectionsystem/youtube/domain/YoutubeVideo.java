package com.realtime.collectionsystem.youtube.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "youtube_videos")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YoutubeVideo {

    @Id
    private String id;

    @Indexed(unique = true)
    private String videoId;

    private String title;

    private String description;

    private List<String> tags;

    private String channelTitle;

    private String categoryId;

    private LocalDateTime createdDate;

    @Indexed
    private LocalDateTime collectedDate;

    @Indexed
    @Builder.Default
    private Boolean publishedToKafka = false;

    public void markAsPublished() {
        this.publishedToKafka = true;
    }
}
