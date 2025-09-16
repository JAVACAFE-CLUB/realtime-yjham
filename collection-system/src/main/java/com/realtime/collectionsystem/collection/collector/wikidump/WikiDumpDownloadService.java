package com.realtime.collectionsystem.collection.collector.wikidump;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class WikiDumpDownloadService {

    private static final String WIKI_DUMP_URL = "https://dumps.wikimedia.org/kowiki/latest/kowiki-latest-pages-articles.xml.bz2";

    @Value("${wiki.dump.storage.path:./wiki-dumps}")
    private String wikiDumpStoragePath;

    public void downloadWikiDumpIfNeeded() {
        try {
            log.info("[WIKI-DUMP] 위키 덤프 다운로드 시작");

            LocalDateTime remoteLastModified = getRemoteFileLastModified();
            if (remoteLastModified == null) {
                log.error("[WIKI-DUMP] 원격 파일의 마지막 수정 시간을 가져올 수 없습니다");
                return;
            }

            Path localFilePath = getLocalFilePath();
            if (shouldSkipDownload(localFilePath, remoteLastModified)) {
                log.info("[WIKI-DUMP] 로컬 파일이 최신 상태입니다. 다운로드를 건너뜁니다");
                return;
            }

            downloadWikiDump(localFilePath);
            log.info("[WIKI-DUMP] 위키 덤프 다운로드 완료");

        } catch (Exception e) {
            log.error("[WIKI-DUMP] 위키 덤프 다운로드 실패: {}", e.getMessage(), e);
            throw new RuntimeException("위키 덤프 다운로드 중 오류가 발생했습니다", e);
        }
    }

    private LocalDateTime getRemoteFileLastModified() throws IOException {
        URL url = new URL(WIKI_DUMP_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("HEAD");
        connection.connect();

        long lastModified = connection.getLastModified();
        if (lastModified == 0) {
            return null;
        }

        return LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(lastModified),
            ZoneId.systemDefault()
        );
    }

    private Path getLocalFilePath() throws IOException {
        Path directory = Paths.get(wikiDumpStoragePath);
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
            log.info("[WIKI-DUMP] 저장 디렉토리 생성: {}", directory.toAbsolutePath());
        }
        return directory.resolve("kowiki-latest-pages-articles.xml.bz2");
    }

    private boolean shouldSkipDownload(Path localFilePath, LocalDateTime remoteLastModified) throws IOException {
        if (!Files.exists(localFilePath)) {
            log.info("[WIKI-DUMP] 로컬 파일이 존재하지 않습니다. 다운로드를 진행합니다");
            return false;
        }

        LocalDateTime localLastModified = LocalDateTime.ofInstant(
            Files.getLastModifiedTime(localFilePath).toInstant(),
            ZoneId.systemDefault()
        );

        log.info("[WIKI-DUMP] 로컬 파일 수정 시간: {}", localLastModified.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        log.info("[WIKI-DUMP] 원격 파일 수정 시간: {}", remoteLastModified.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        return !remoteLastModified.isAfter(localLastModified);
    }

    private void downloadWikiDump(Path localFilePath) throws IOException {
        log.info("[WIKI-DUMP] 파일 다운로드 시작: {}", WIKI_DUMP_URL);

        URL url = new URL(WIKI_DUMP_URL);
        try (var inputStream = url.openStream()) {
            Files.copy(inputStream, localFilePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("[WIKI-DUMP] 파일 다운로드 완료: {}", localFilePath.toAbsolutePath());
        }
    }

    public Path getDownloadedFilePath() throws IOException {
        return getLocalFilePath();
    }
}