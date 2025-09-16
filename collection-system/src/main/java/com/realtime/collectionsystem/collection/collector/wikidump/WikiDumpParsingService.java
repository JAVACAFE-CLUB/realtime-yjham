package com.realtime.collectionsystem.collection.collector.wikidump;

import com.realtime.collectionsystem.domain.WikiPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class WikiDumpParsingService {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final Pattern CATEGORY_PATTERN = Pattern.compile("\\[\\[분류:([^\\]]+)\\]\\]");
    private static final Pattern INTERNAL_LINK_PATTERN = Pattern.compile("\\[\\[([^\\]|]+)");
    private static final Pattern EXTERNAL_LINK_PATTERN = Pattern.compile("\\[https?://[^\\s\\]]+");

    public void parseWikiDump(Path xmlFilePath, Consumer<List<WikiPage>> pageConsumer, int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("배치 크기는 1 이상이어야 합니다");
        }

        try {
            log.info("[WIKI-DUMP] XML 파싱 시작: {}, 배치 크기: {}", xmlFilePath, batchSize);

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();

            WikiPageHandler handler = new WikiPageHandler(pageConsumer, batchSize);

            try (FileInputStream fis = new FileInputStream(xmlFilePath.toFile())) {
                parser.parse(fis, handler);
            }

            // 마지막 배치 처리
            handler.flushBatch();

            log.info("[WIKI-DUMP] XML 파싱 완료 - 총 {}개 페이지 처리", handler.getTotalProcessed());

        } catch (Exception e) {
            log.error("[WIKI-DUMP] XML 파싱 실패: {}", e.getMessage(), e);
            throw new RuntimeException("XML 파싱 중 오류가 발생했습니다", e);
        }
    }

    private static class WikiPageHandler extends DefaultHandler {
        private final Consumer<List<WikiPage>> pageConsumer;
        private final int batchSize;
        private final List<WikiPage> currentBatch;

        private StringBuilder textBuffer;
        private boolean inPage = false;
        private boolean inRevision = false;
        private boolean inContributor = false;

        // Page fields
        private String title;
        private Integer namespace;
        private Long pageId;

        // Revision fields
        private Long revisionId;
        private String timestamp;
        private String contributor;
        private Long contributorId;
        private String comment;
        private String content;
        private String sha1;

        private long totalProcessed = 0;

        public WikiPageHandler(Consumer<List<WikiPage>> pageConsumer, int batchSize) {
            this.pageConsumer = pageConsumer;
            this.batchSize = batchSize;
            this.currentBatch = new ArrayList<>(batchSize);
            this.textBuffer = new StringBuilder();
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            textBuffer.setLength(0);

            switch (qName) {
                case "page":
                    inPage = true;
                    resetPageFields();
                    break;
                case "revision":
                    inRevision = true;
                    break;
                case "contributor":
                    inContributor = true;
                    break;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            String text = textBuffer.toString().trim();

            if (inPage && !inRevision) {
                // Page level elements
                switch (qName) {
                    case "title":
                        title = text;
                        break;
                    case "ns":
                        try {
                            namespace = Integer.parseInt(text);
                        } catch (NumberFormatException e) {
                            namespace = null;
                        }
                        break;
                    case "id":
                        try {
                            pageId = Long.parseLong(text);
                        } catch (NumberFormatException e) {
                            pageId = null;
                        }
                        break;
                    case "page":
                        inPage = false;
                        processCompletePage();
                        break;
                }
            } else if (inRevision) {
                // Revision level elements
                switch (qName) {
                    case "id":
                        if (!inContributor) {
                            try {
                                revisionId = Long.parseLong(text);
                            } catch (NumberFormatException e) {
                                revisionId = null;
                            }
                        } else {
                            try {
                                contributorId = Long.parseLong(text);
                            } catch (NumberFormatException e) {
                                contributorId = null;
                            }
                        }
                        break;
                    case "timestamp":
                        timestamp = text;
                        break;
                    case "username":
                        contributor = text;
                        break;
                    case "comment":
                        comment = text;
                        break;
                    case "text":
                        content = text;
                        break;
                    case "sha1":
                        sha1 = text;
                        break;
                    case "contributor":
                        inContributor = false;
                        break;
                    case "revision":
                        inRevision = false;
                        break;
                }
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            textBuffer.append(ch, start, length);
        }

        private void resetPageFields() {
            title = null;
            namespace = null;
            pageId = null;
            revisionId = null;
            timestamp = null;
            contributor = null;
            contributorId = null;
            comment = null;
            content = null;
            sha1 = null;
        }

        private void processCompletePage() {
            if (isValidPage()) {
                try {
                    WikiPage wikiPage = buildWikiPage();
                    currentBatch.add(wikiPage);
                    totalProcessed++;

                    if (currentBatch.size() >= batchSize) {
                        flushBatch();
                    }

                    if (totalProcessed % 10000 == 0) {
                        log.info("[WIKI-DUMP] 파싱 진행 중... {}개 페이지 처리 완료", totalProcessed);
                    }

                } catch (Exception e) {
                    log.warn("[WIKI-DUMP] 페이지 처리 실패 - ID: {}, 제목: {}, 오류: {}", pageId, title, e.getMessage());
                }
            }
        }

        private boolean isValidPage() {
            return pageId != null && title != null && content != null;
        }

        private WikiPage buildWikiPage() {
            LocalDateTime lastModified = parseTimestamp(timestamp);
            LocalDateTime collectedDate = LocalDateTime.now();

            List<String> categories = extractCategories(content);
            List<String> internalLinks = extractInternalLinks(content);
            List<String> externalLinks = extractExternalLinks(content);

            return WikiPage.builder()
                    .pageId(pageId)
                    .title(title)
                    .content(content)
                    .namespace(namespace)
                    .namespaceName(getNamespaceName(namespace))
                    .revisionId(revisionId)
                    .lastModified(lastModified)
                    .contributor(contributor)
                    .contributorId(contributorId)
                    .editComment(comment)
                    .contentLength(content != null ? content.length() : 0)
                    .sha1Hash(sha1)
                    .categories(categories)
                    .internalLinks(internalLinks)
                    .externalLinks(externalLinks)
                    .collectedDate(collectedDate)
                    .build();
        }

        private LocalDateTime parseTimestamp(String timestamp) {
            if (timestamp == null || timestamp.isEmpty()) {
                return null;
            }
            try {
                return LocalDateTime.parse(timestamp, ISO_FORMATTER);
            } catch (DateTimeParseException e) {
                log.warn("[WIKI-DUMP] 타임스탬프 파싱 실패: {}", timestamp);
                return null;
            }
        }

        private String getNamespaceName(Integer namespace) {
            if (namespace == null) return null;

            switch (namespace) {
                case 0: return "일반";
                case 1: return "토론";
                case 2: return "사용자";
                case 3: return "사용자토론";
                case 4: return "위키백과";
                case 6: return "파일";
                case 10: return "틀";
                case 14: return "분류";
                default: return "기타(" + namespace + ")";
            }
        }

        private List<String> extractCategories(String content) {
            if (content == null) return new ArrayList<>();

            List<String> categories = new ArrayList<>();
            Matcher matcher = CATEGORY_PATTERN.matcher(content);
            while (matcher.find()) {
                categories.add(matcher.group(1).trim());
            }
            return categories;
        }

        private List<String> extractInternalLinks(String content) {
            if (content == null) return new ArrayList<>();

            List<String> links = new ArrayList<>();
            Matcher matcher = INTERNAL_LINK_PATTERN.matcher(content);
            while (matcher.find()) {
                String link = matcher.group(1).trim();
                if (!link.startsWith("분류:") && !link.startsWith("파일:")) {
                    links.add(link);
                }
            }
            return links;
        }

        private List<String> extractExternalLinks(String content) {
            if (content == null) return new ArrayList<>();

            List<String> links = new ArrayList<>();
            Matcher matcher = EXTERNAL_LINK_PATTERN.matcher(content);
            while (matcher.find()) {
                links.add(matcher.group().replaceAll("^\\[|\\]$", "").trim());
            }
            return links;
        }

        public void flushBatch() {
            if (!currentBatch.isEmpty()) {
                pageConsumer.accept(new ArrayList<>(currentBatch));
                currentBatch.clear();
            }
        }

        public long getTotalProcessed() {
            return totalProcessed;
        }
    }
}