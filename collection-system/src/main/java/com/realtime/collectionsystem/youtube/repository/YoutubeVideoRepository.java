package com.realtime.collectionsystem.youtube.repository;

import com.realtime.collectionsystem.youtube.domain.YoutubeVideo;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface YoutubeVideoRepository extends MongoRepository<YoutubeVideo, String> {

    List<YoutubeVideo> findByPublishedToKafkaFalse();

    boolean existsByVideoId(String videoId);
}
