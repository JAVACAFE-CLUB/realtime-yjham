package com.realtime.cleansingsystem.wiki.repository;

import com.realtime.cleansingsystem.wiki.domain.WikiPage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * 위키피디아 원본 데이터 조회 Repository
 * collection database에서 조회
 */
public interface WikiPageRepository extends MongoRepository<WikiPage, String> {

    Optional<WikiPage> findByTitle(String title);
}
