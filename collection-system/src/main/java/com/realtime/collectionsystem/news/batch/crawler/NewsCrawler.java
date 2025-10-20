package com.realtime.collectionsystem.news.batch.crawler;

import com.realtime.collectionsystem.news.domain.NewsArticle;

public interface NewsCrawler {

    String getSource();

    NewsArticle crawl(String url);
}
