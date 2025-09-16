package com.realtime.collectionsystem.messaging.service;

import com.realtime.collectionsystem.domain.WikiPage;
import com.realtime.collectionsystem.messaging.dto.WikiPageCollectionEvent;
import com.realtime.collectionsystem.messaging.producer.WikiPageCollectionEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WikiPageEventService {

    private final WikiPageCollectionEventProducer eventProducer;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final String EVENT_TYPE = "WIKIPAGE_COLLECTED";
    private static final String VERSION = "1.0";

    public void publishCollectionEvents(List<WikiPage> wikiPages) {
        if (wikiPages == null || wikiPages.isEmpty()) {
            log.warn("발송할 위키페이지가 없습니다.");
            return;
        }

        log.info("위키페이지 수집 이벤트 발송 시작: {}개", wikiPages.size());

        try {
            List<WikiPageCollectionEvent> events = wikiPages.stream()
                    .map(this::createEvent)
                    .collect(Collectors.toList());

            eventProducer.sendWikiPageCollectionEvents(events);

            log.info("위키페이지 수집 이벤트 발송 완료: {}개", events.size());

        } catch (Exception e) {
            log.error("위키페이지 수집 이벤트 발송 중 오류 발생", e);
            throw new RuntimeException("위키페이지 수집 이벤트 발송 실패", e);
        }
    }

    public void publishCollectionEvent(WikiPage wikiPage) {
        if (wikiPage == null) {
            log.warn("발송할 위키페이지가 null입니다.");
            return;
        }

        try {
            WikiPageCollectionEvent event = createEvent(wikiPage);
            eventProducer.sendWikiPageCollectionEvent(event);

            log.debug("위키페이지 수집 이벤트 발송 완료 - 페이지 ID: {}", wikiPage.getPageId());

        } catch (Exception e) {
            log.error("위키페이지 수집 이벤트 발송 중 오류 발생 - 페이지 ID: {}, 제목: {}",
                     wikiPage.getPageId(), wikiPage.getTitle(), e);
            throw new RuntimeException("위키페이지 수집 이벤트 발송 실패", e);
        }
    }

    private WikiPageCollectionEvent createEvent(WikiPage wikiPage) {
        String pageId = generatePageId(wikiPage);

        return WikiPageCollectionEvent.builder()
                .pageId(pageId)
                .wikiPageId(wikiPage.getPageId())
                .title(wikiPage.getTitle())
                .namespace(wikiPage.getNamespace())
                .namespaceName(wikiPage.getNamespaceName())
                .revisionId(wikiPage.getRevisionId())
                .lastModified(wikiPage.getLastModified())
                .contributor(wikiPage.getContributor())
                .collectedDate(wikiPage.getCollectedDate())
                .contentLength(wikiPage.getContentLength() != null ? wikiPage.getContentLength() : 0)
                .categories(wikiPage.getCategories())
                .internalLinksCount(wikiPage.getInternalLinks() != null ? wikiPage.getInternalLinks().size() : 0)
                .externalLinksCount(wikiPage.getExternalLinks() != null ? wikiPage.getExternalLinks().size() : 0)
                .eventType(EVENT_TYPE)
                .version(VERSION)
                .build();
    }

    private String generatePageId(WikiPage wikiPage) {
        String collectedDate = wikiPage.getCollectedDate().format(DATE_FORMATTER);
        String namespace = getNamespaceName(wikiPage.getNamespace());
        String safeTitle = sanitizeFileName(wikiPage.getTitle());

        return String.format("%s/namespace-%s/page-%d-%s",
                collectedDate, namespace, wikiPage.getPageId(), safeTitle);
    }

    private String sanitizeFileName(String title) {
        if (title == null) return "unknown";

        return title.replaceAll("[\\\\/:*?\"<>|]", "_")
                   .replaceAll("\\s+", "_")
                   .substring(0, Math.min(title.length(), 50));
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