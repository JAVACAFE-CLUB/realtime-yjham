package com.realtime.collectionsystem.collection.scheduler;

import com.realtime.collectionsystem.collection.collector.wikidump.WikiDumpDownloadService;
import com.realtime.collectionsystem.collection.collector.wikidump.WikiDumpDecompressionService;
import com.realtime.collectionsystem.collection.collector.wikidump.WikiDumpParsingService;
import com.realtime.collectionsystem.storage.service.WikiPageStorageService;
import com.realtime.collectionsystem.messaging.service.WikiPageEventService;
import com.realtime.collectionsystem.domain.WikiPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class WikiDumpScheduler {

    private final WikiDumpDownloadService wikiDumpDownloadService;
    private final WikiDumpDecompressionService wikiDumpDecompressionService;
    private final WikiDumpParsingService wikiDumpParsingService;
    private final WikiPageStorageService wikiPageStorageService;
    private final WikiPageEventService wikiPageEventService;

    @Value("${wiki.dump.processing.batch-size:1000}")
    private int batchSize;

    @Scheduled(fixedRate = 7, timeUnit = TimeUnit.DAYS, initialDelay = 0)
    public void scheduledWikiDataCollection() {
        log.info("[WIKI-DUMP] 예약된 위키 데이터 수집 실행 시작");
        executeWikiDataCollection();
    }

    private void executeWikiDataCollection() {
        long startTime = System.currentTimeMillis();
        Path decompressedFilePath = null;

        try {
            // 1. 위키 덤프 파일 다운로드
            log.info("[WIKI-DUMP] 1단계: 위키 덤프 파일 다운로드");
            wikiDumpDownloadService.downloadWikiDumpIfNeeded();
            Path downloadedFilePath = wikiDumpDownloadService.getDownloadedFilePath();

            // 2. BZ2 파일 압축 해제
            log.info("[WIKI-DUMP] 2단계: BZ2 파일 압축 해제");
            decompressedFilePath = wikiDumpDecompressionService.decompressBz2File(downloadedFilePath);

            // 3. XML 파일 파싱 및 데이터 추출, 저장, 이벤트 발송
            log.info("[WIKI-DUMP] 3단계: XML 파싱 및 데이터 처리 (배치 크기: {})", batchSize);
            wikiDumpParsingService.parseWikiDump(decompressedFilePath, this::processBatch, batchSize);

            long duration = System.currentTimeMillis() - startTime;
            log.info("[WIKI-DUMP] 위키 데이터 수집 완료 - 소요시간: {}ms", duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[WIKI-DUMP] 위키 데이터 수집 실패 - 소요시간: {}ms, 오류: {}", duration, e.getMessage(), e);
        } finally {
            // 4. 임시 압축 해제 파일 정리 (선택적)
            if (decompressedFilePath != null) {
                wikiDumpDecompressionService.cleanupDecompressedFile(decompressedFilePath);
            }
        }
    }

    private void processBatch(List<WikiPage> wikiPages) {
        if (wikiPages == null || wikiPages.isEmpty()) {
            return;
        }

        try {
            log.debug("[WIKI-DUMP] 배치 처리 시작: {}개 페이지", wikiPages.size());

            // MinIO에 저장
            wikiPageStorageService.saveWikiPages(wikiPages);

            // 카프카 이벤트 발송
            wikiPageEventService.publishCollectionEvents(wikiPages);

            log.debug("[WIKI-DUMP] 배치 처리 완료: {}개 페이지", wikiPages.size());

        } catch (Exception e) {
            log.error("[WIKI-DUMP] 배치 처리 실패: {}개 페이지, 오류: {}", wikiPages.size(), e.getMessage(), e);
            // 배치 처리 실패 시에도 계속 진행
        }
    }
}