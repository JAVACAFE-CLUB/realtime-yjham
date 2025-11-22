package com.realtime.trend.processing.dto;

import java.time.LocalDateTime;
import java.util.List;

public record RawNewsMessage(
        String id,
        String url,
        String title,
        String content,
        LocalDateTime publishedAt,
        String publisher,
        String author,
        String category,
        List<String> tags,
        LocalDateTime collectedAt
) {
}
