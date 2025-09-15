package com.realtime.collectionsystem.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Builder
public class Article {
    private final String url;
    private final String title;
    private final String content;
    private final String author;
    private final String source;
    private final LocalDateTime publishedDate;
    private final LocalDateTime collectedDate;

    public Article(String url, String title, String content, String author, String source, LocalDateTime publishedDate, LocalDateTime collectedDate) {
        this.url = Objects.requireNonNull(url, "URL cannot be null");
        this.title = Objects.requireNonNull(title, "Title cannot be null");
        this.content = content;
        this.author = author;
        this.source = source;
        this.publishedDate = publishedDate;
        this.collectedDate = Objects.requireNonNull(collectedDate, "Collected time cannot be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Article article = (Article) o;
        return Objects.equals(url, article.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }
}