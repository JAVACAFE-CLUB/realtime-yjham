package com.realtime.collectionsystem.collection.collector.wikidump;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
public class WikiDumpDecompressionService {

    public Path decompressBz2File(Path compressedFilePath) {
        if (!Files.exists(compressedFilePath)) {
            throw new IllegalArgumentException("압축 파일이 존재하지 않습니다: " + compressedFilePath);
        }

        if (!compressedFilePath.toString().endsWith(".bz2")) {
            throw new IllegalArgumentException("BZ2 파일이 아닙니다: " + compressedFilePath);
        }

        try {
            log.info("[WIKI-DUMP] BZ2 파일 압축 해제 시작: {}", compressedFilePath);

            Path outputPath = getDecompressedFilePath(compressedFilePath);

            if (shouldSkipDecompression(compressedFilePath, outputPath)) {
                log.info("[WIKI-DUMP] 압축 해제된 파일이 이미 최신 상태입니다: {}", outputPath);
                return outputPath;
            }

            decompressFile(compressedFilePath, outputPath);

            log.info("[WIKI-DUMP] BZ2 파일 압축 해제 완료: {}", outputPath);
            return outputPath;

        } catch (Exception e) {
            log.error("[WIKI-DUMP] BZ2 파일 압축 해제 실패: {}", e.getMessage(), e);
            throw new RuntimeException("BZ2 파일 압축 해제 중 오류가 발생했습니다", e);
        }
    }

    private Path getDecompressedFilePath(Path compressedFilePath) {
        String fileName = compressedFilePath.getFileName().toString();
        String decompressedFileName = fileName.substring(0, fileName.lastIndexOf(".bz2"));
        return compressedFilePath.getParent().resolve(decompressedFileName);
    }

    private boolean shouldSkipDecompression(Path compressedFilePath, Path outputPath) throws IOException {
        if (!Files.exists(outputPath)) {
            log.info("[WIKI-DUMP] 압축 해제된 파일이 존재하지 않습니다. 압축 해제를 진행합니다");
            return false;
        }

        long compressedLastModified = Files.getLastModifiedTime(compressedFilePath).toMillis();
        long decompressedLastModified = Files.getLastModifiedTime(outputPath).toMillis();

        // 압축 파일이 더 최신이면 다시 압축 해제 필요
        return compressedLastModified <= decompressedLastModified;
    }

    private void decompressFile(Path compressedFilePath, Path outputPath) throws IOException {
        try (FileInputStream fis = new FileInputStream(compressedFilePath.toFile());
             BufferedInputStream bis = new BufferedInputStream(fis);
             BZip2CompressorInputStream bzis = new BZip2CompressorInputStream(bis);
             FileOutputStream fos = new FileOutputStream(outputPath.toFile());
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {

            log.info("[WIKI-DUMP] 파일 복사 시작 - 원본: {}, 대상: {}", compressedFilePath, outputPath);

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = 0;

            while ((bytesRead = bzis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;

                // 100MB마다 진행 상황 로그
                if (totalBytes % (100 * 1024 * 1024) == 0) {
                    log.info("[WIKI-DUMP] 압축 해제 진행 중... {}MB 처리 완료", totalBytes / (1024 * 1024));
                }
            }

            log.info("[WIKI-DUMP] 파일 복사 완료 - 총 {}MB 처리", totalBytes / (1024 * 1024));
        }
    }

    public boolean isDecompressed(Path filePath) {
        return Files.exists(filePath) && !filePath.toString().endsWith(".bz2");
    }

    public void cleanupDecompressedFile(Path decompressedFilePath) {
        try {
            if (Files.exists(decompressedFilePath)) {
                Files.delete(decompressedFilePath);
                log.info("[WIKI-DUMP] 압축 해제된 파일 정리 완료: {}", decompressedFilePath);
            }
        } catch (IOException e) {
            log.warn("[WIKI-DUMP] 압축 해제된 파일 정리 실패: {}", e.getMessage());
        }
    }
}