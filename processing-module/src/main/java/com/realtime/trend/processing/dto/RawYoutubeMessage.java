package com.realtime.trend.processing.dto;

import java.time.LocalDateTime;
import java.util.List;

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
