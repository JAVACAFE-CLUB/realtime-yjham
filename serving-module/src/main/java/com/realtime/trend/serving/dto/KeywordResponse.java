package com.realtime.trend.serving.dto;

import java.time.LocalDateTime;
import java.util.List;

public record KeywordResponse(
        List<KeywordItem> keywords,
        Metadata metadata
) {
    public record KeywordItem(
            String keyword,
            String type,
            String category,
            long count
    ) {
    }

    public record Metadata(
            int totalCount,
            int limit,
            String source,
            String type,
            String category,
            LocalDateTime lastUpdated
    ) {
    }
}
