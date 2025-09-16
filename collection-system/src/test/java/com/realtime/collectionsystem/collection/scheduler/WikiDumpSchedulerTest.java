package com.realtime.collectionsystem.collection.scheduler;

import com.realtime.collectionsystem.collection.collector.wikidump.WikiDumpDownloadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WikiDumpSchedulerTest {

    @Mock
    private WikiDumpDownloadService wikiDumpDownloadService;

    @InjectMocks
    private WikiDumpScheduler wikiDumpScheduler;

    @Test
    void testScheduledWikiDataCollection() {
        doNothing().when(wikiDumpDownloadService).downloadWikiDumpIfNeeded();

        assertDoesNotThrow(() -> wikiDumpScheduler.scheduledWikiDataCollection());

        verify(wikiDumpDownloadService).downloadWikiDumpIfNeeded();
    }
}