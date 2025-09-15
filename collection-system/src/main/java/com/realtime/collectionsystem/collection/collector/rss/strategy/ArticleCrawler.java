package com.realtime.collectionsystem.collection.collector.rss.strategy;

import com.realtime.collectionsystem.domain.Article;

public interface ArticleCrawler {

    Article crawlArticle(String url);

    boolean canHandle(String url);

    String getSourceName();
}