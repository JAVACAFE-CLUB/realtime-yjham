package com.realtime.collectionsystem.storage.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.realtime.collectionsystem.domain.Article;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MinIOArticleRepository implements ArticleRepository {

    private final MinioClient minioClient;
    private final ObjectMapper objectMapper;

    private static final String BUCKET_NAME = "crawled-data";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @PostConstruct
    public void initializeBucket() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(BUCKET_NAME)
                            .build()
            );

            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(BUCKET_NAME)
                                .build()
                );
                log.info("MinIO 버킷 생성 완료: {}", BUCKET_NAME);
            } else {
                log.info("MinIO 버킷 이미 존재: {}", BUCKET_NAME);
            }
        } catch (Exception e) {
            log.error("MinIO 버킷 초기화 실패: {}", BUCKET_NAME, e);
            throw new RuntimeException("MinIO 버킷 초기화 중 오류 발생", e);
        }
    }

    @Override
    public void save(Article article) {
        try {
            String objectName = generateObjectName(article);
            String jsonContent = objectMapper.writeValueAsString(article);
            byte[] jsonBytes = jsonContent.getBytes(StandardCharsets.UTF_8);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(objectName)
                            .stream(new ByteArrayInputStream(jsonBytes),
                                   jsonBytes.length, -1)
                            .contentType("application/json; charset=utf-8")
                            .build()
            );

            log.debug("기사 저장 완료: {}", objectName);
        } catch (Exception e) {
            log.error("MinIO 기사 저장 실패: {}", article.getUrl(), e);
            throw new RuntimeException("기사 저장 중 오류 발생", e);
        }
    }

    @Override
    public void saveAll(List<Article> articles) {
        articles.forEach(this::save);
        log.info("MinIO에 {}개 기사 저장 완료", articles.size());
    }

    private String generateObjectName(Article article) {
        String datePrefix = article.getCollectedDate().format(DATE_FORMATTER);
        String urlHash = String.valueOf(article.getUrl().hashCode());
        String source = article.getSource().replace(" ", "_");
        return String.format("articles/%s/%s/%s.json", datePrefix, source, urlHash);
    }
}
