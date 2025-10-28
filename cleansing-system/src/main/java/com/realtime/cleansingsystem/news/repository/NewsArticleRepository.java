package com.realtime.cleansingsystem.news.repository;

import com.realtime.cleansingsystem.news.domain.NewsArticle;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * 뉴스 원본 데이터 조회 Repository
 * collection database에서 조회
 */
public interface NewsArticleRepository extends MongoRepository<NewsArticle, String> {

    Optional<NewsArticle> findByUrl(String url);
}
