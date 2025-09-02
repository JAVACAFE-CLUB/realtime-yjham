package com.yjham.realtime.collection.crawler;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record NewsData(
        String title,
        String content,
        String url,
        String publishTime,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime crawledAt
) {
    
    public NewsData {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("제목은 필수입니다");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("내용은 필수입니다");
        }
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL은 필수입니다");
        }
        if (crawledAt == null) {
            throw new IllegalArgumentException("크롤링 시간은 필수입니다");
        }
    }
}