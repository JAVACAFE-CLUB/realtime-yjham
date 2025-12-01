package com.realtime.trend.processing.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RawYoutubeMessage(
        String id,
        String videoId,
        String title,
        String description,
        String channelTitle,
        LocalDateTime publishedAt,
        String categoryId,
        List<String> tags,
        Long viewCount,
        Long likeCount,
        Long commentCount,
        LocalDateTime collectedAt
) {
}
