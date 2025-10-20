package com.realtime.collectionsystem.news.batch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RssFeedItem {

    private String source;
    private String url;
    private String category;
    private String publishDate;
}
