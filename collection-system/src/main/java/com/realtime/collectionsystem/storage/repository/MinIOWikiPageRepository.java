package com.realtime.collectionsystem.storage.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.realtime.collectionsystem.domain.WikiPage;
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
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MinIOWikiPageRepository implements WikiPageRepository {

    private final MinioClient minioClient;
    private final ObjectMapper objectMapper;

    private static final String BUCKET_NAME = "wiki-data";
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
                log.info("MinIO 위키 버킷 생성 완료: {}", BUCKET_NAME);
            } else {
                log.info("MinIO 위키 버킷 이미 존재: {}", BUCKET_NAME);
            }

        } catch (Exception e) {
            log.error("MinIO 위키 버킷 초기화 실패", e);
            throw new RuntimeException("MinIO 위키 버킷 초기화 중 오류가 발생했습니다", e);
        }
    }

    @Override
    public void save(WikiPage wikiPage) {
        try {
            String objectName = generateObjectName(wikiPage);
            String jsonContent = objectMapper.writeValueAsString(wikiPage);

            // JSON 내용 검증
            if (jsonContent == null || jsonContent.isEmpty() || !jsonContent.endsWith("}")) {
                log.error("잘못된 JSON 생성됨 - 페이지 ID: {}, JSON 길이: {}, 마지막 문자: {}",
                        wikiPage.getPageId(),
                        jsonContent != null ? jsonContent.length() : 0,
                        jsonContent != null && jsonContent.length() > 0 ? jsonContent.charAt(jsonContent.length() - 1) : "null");
                throw new RuntimeException("잘못된 JSON 형식이 생성되었습니다");
            }

            byte[] jsonBytes = jsonContent.getBytes(StandardCharsets.UTF_8);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(objectName)
                            .stream(new ByteArrayInputStream(jsonBytes),
                                    jsonBytes.length, -1)
                            .contentType("application/json")
                            .build()
            );

            log.debug("위키페이지 저장 완료: {} (JSON 크기: {}바이트)", objectName, jsonBytes.length);

        } catch (Exception e) {
            log.error("위키페이지 저장 실패 - ID: {}, 제목: {}", wikiPage.getPageId(), wikiPage.getTitle(), e);
            throw new RuntimeException("위키페이지 저장 중 오류가 발생했습니다", e);
        }
    }

    @Override
    public void saveAll(List<WikiPage> wikiPages) {
        if (wikiPages == null || wikiPages.isEmpty()) {
            log.warn("저장할 위키페이지가 없습니다.");
            return;
        }

        try {
            log.info("위키페이지 일괄 저장 시작: {}개", wikiPages.size());

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);

            // 네임스페이스별로 그룹화
            Map<Integer, List<WikiPage>> pagesByNamespace = wikiPages.stream()
                    .collect(Collectors.groupingBy(page -> page.getNamespace() != null ? page.getNamespace() : -1));

            for (Map.Entry<Integer, List<WikiPage>> entry : pagesByNamespace.entrySet()) {
                Integer namespace = entry.getKey();
                List<WikiPage> pages = entry.getValue();

                log.info("네임스페이스 {} 위키페이지 저장 중: {}개", getNamespaceName(namespace), pages.size());

                for (WikiPage page : pages) {
                    try {
                        save(page);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                        log.warn("위키페이지 저장 실패 - ID: {}, 제목: {}, 오류: {}",
                                page.getPageId(), page.getTitle(), e.getMessage());
                    }
                }
            }

            log.info("위키페이지 일괄 저장 완료 - 성공: {}개, 실패: {}개", successCount.get(), failureCount.get());

        } catch (Exception e) {
            log.error("위키페이지 일괄 저장 중 오류 발생", e);
            throw new RuntimeException("위키페이지 일괄 저장 중 오류가 발생했습니다", e);
        }
    }

    private String generateObjectName(WikiPage wikiPage) {
        String collectedDate = wikiPage.getCollectedDate().format(DATE_FORMATTER);
        String namespace = getNamespaceName(wikiPage.getNamespace());

        // 파일명에 사용할 수 없는 문자들을 제거/변환
        String safeTitle = sanitizeFileName(wikiPage.getTitle());

        return String.format("%s/namespace-%s/page-%d-%s.json",
                collectedDate, namespace, wikiPage.getPageId(), safeTitle);
    }

    private String sanitizeFileName(String title) {
        if (title == null) return "unknown";

        return title.replaceAll("[\\\\/:*?\"<>|]", "_")
                   .replaceAll("\\s+", "_")
                   .substring(0, Math.min(title.length(), 50)); // 파일명 길이 제한
    }

    private String getNamespaceName(Integer namespace) {
        if (namespace == null) return "unknown";

        switch (namespace) {
            case 0: return "main";
            case 1: return "talk";
            case 2: return "user";
            case 3: return "user_talk";
            case 4: return "wikipedia";
            case 6: return "file";
            case 10: return "template";
            case 14: return "category";
            default: return "ns_" + namespace;
        }
    }
}