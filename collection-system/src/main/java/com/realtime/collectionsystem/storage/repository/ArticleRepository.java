package com.realtime.collectionsystem.storage.repository;

import com.realtime.collectionsystem.domain.Article;

public interface ArticleRepository {

    void save(Article article);

    void saveAll(java.util.List<Article> articles);
}
