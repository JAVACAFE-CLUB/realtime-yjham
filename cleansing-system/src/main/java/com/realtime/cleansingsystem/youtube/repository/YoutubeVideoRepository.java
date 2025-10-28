package com.realtime.cleansingsystem.youtube.repository;

import com.realtime.cleansingsystem.youtube.domain.YoutubeVideo;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * 유튜브 원본 데이터 조회 Repository
 * collection database에서 조회
 */
public interface YoutubeVideoRepository extends MongoRepository<YoutubeVideo, String> {

    Optional<YoutubeVideo> findByVideoId(String videoId);
}
