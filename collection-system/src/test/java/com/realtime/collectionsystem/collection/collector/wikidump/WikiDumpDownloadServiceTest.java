package com.realtime.collectionsystem.collection.collector.wikidump;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WikiDumpDownloadServiceTest {

    @TempDir
    Path tempDir;

    private WikiDumpDownloadService wikiDumpDownloadService;

    @BeforeEach
    void setUp() {
        wikiDumpDownloadService = new WikiDumpDownloadService();
        ReflectionTestUtils.setField(wikiDumpDownloadService, "wikiDumpStoragePath", tempDir.toString());
    }

    @Test
    void testGetDownloadedFilePath() {
        assertDoesNotThrow(() -> {
            Path filePath = wikiDumpDownloadService.getDownloadedFilePath();
            assertTrue(filePath.toString().contains("kowiki-latest-pages-articles.xml.bz2"));
        });
    }
}