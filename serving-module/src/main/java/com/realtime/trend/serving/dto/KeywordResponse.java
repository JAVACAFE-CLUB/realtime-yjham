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
            long count
    ) {
    }

    public record Metadata(
            int totalCount,
            int limit,
            String source,
            String type,
            LocalDateTime lastUpdated
    ) {
    }
}
