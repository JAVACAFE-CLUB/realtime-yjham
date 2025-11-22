package com.realtime.trend.indexing.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ProcessedMessage(
        String id,
        String sourceId,
        String title,
        String processedContent,
        LocalDateTime collectedAt,
        List<ExtractedEntity> keywords,
        String source
) {
    public record ExtractedEntity(String keyword, String type) {
    }
}
