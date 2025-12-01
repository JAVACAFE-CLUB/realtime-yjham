package com.realtime.trend.processing.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
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
