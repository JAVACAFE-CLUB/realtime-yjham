package com.realtime.trend.processing.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ProcessedYoutubeMessage(
        String id,
        String videoId,
        String title,
        String processedDescription,
        String channelTitle,
        LocalDateTime publishedAt,
        String categoryId,
        List<String> tags,
        Long viewCount,
        Long likeCount,
        Long commentCount,
        LocalDateTime collectedAt,
        List<ExtractedEntity> keywords
) {
}
