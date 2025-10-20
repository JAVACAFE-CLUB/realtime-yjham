package com.realtime.collectionsystem.wiki.repository;

import com.realtime.collectionsystem.wiki.domain.WikiPage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface WikiPageRepository extends MongoRepository<WikiPage, String> {

    List<WikiPage> findByPublishedToKafkaFalse();

    Optional<WikiPage> findByTitle(String title);
}
