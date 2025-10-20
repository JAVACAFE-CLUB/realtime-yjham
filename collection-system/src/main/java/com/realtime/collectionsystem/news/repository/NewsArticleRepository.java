package com.realtime.collectionsystem.news.repository;

import com.realtime.collectionsystem.news.domain.NewsArticle;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NewsArticleRepository extends MongoRepository<NewsArticle, String> {

    List<NewsArticle> findByPublishedToKafkaFalse();

    boolean existsByUrl(String url);
}
