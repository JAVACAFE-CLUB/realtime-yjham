package com.realtime.trend.processing.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ProcessedNewsMessage(
        String id,
        String url,
        String title,
        String processedContent,
        LocalDateTime publishedAt,
        String publisher,
        String author,
        String category,
        List<String> tags,
        LocalDateTime collectedAt,
        List<ExtractedEntity> keywords
) {
}
