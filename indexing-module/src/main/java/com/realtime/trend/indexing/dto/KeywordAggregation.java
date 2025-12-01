package com.realtime.trend.indexing.dto;

public record KeywordAggregation(
        String keyword,
        String type,
        String category,
        long count
) {
}
