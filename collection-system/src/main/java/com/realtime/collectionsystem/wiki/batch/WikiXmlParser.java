package com.realtime.collectionsystem.wiki.batch;

import com.realtime.collectionsystem.common.util.DateTimeUtils;
import com.realtime.collectionsystem.wiki.domain.WikiPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.ctc.wstx.stax.WstxInputFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class WikiXmlParser {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public List<WikiPage> parse(InputStream inputStream) throws XMLStreamException {
        List<WikiPage> pages = new ArrayList<>();
        // Woodstox 파서 설정 - 대용량 위키피디아 XML 처리 최적화
        WstxInputFactory factory = new WstxInputFactory();
        
        // Woodstox 전용: 큰 텍스트 엔티티 처리 (위키피디아 페이지 본문은 수 MB까지 가능)
        factory.getConfig().setMaxTextLength(10_000_000);  // 10MB
        
        // 성능 최적화
        factory.setProperty(XMLInputFactory.IS_COALESCING, true);  // 인접한 텍스트 노드 병합
        
        XMLEventReader reader = factory.createXMLEventReader(inputStream);

        WikiPageBuilder builder = null;

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement()) {
                StartElement startElement = event.asStartElement();
                String elementName = startElement.getName().getLocalPart();

                switch (elementName) {
                    case "page":
                        builder = new WikiPageBuilder();
                        break;
                    case "title":
                        if (builder != null) {
                            builder.title = getElementText(reader);
                        }
                        break;
                    case "ns":
                        if (builder != null) {
                            builder.namespace = Integer.parseInt(getElementText(reader));
                        }
                        break;
                    case "id":
                        if (builder != null && builder.pageId == null) {
                            builder.pageId = Long.parseLong(getElementText(reader));
                        } else if (builder != null && builder.insideRevision && builder.revisionId == null) {
                            builder.revisionId = Long.parseLong(getElementText(reader));
                        }
                        break;
                    case "redirect":
                        if (builder != null) {
                            builder.isRedirect = true;
                        }
                        break;
                    case "revision":
                        if (builder != null) {
                            builder.insideRevision = true;
                        }
                        break;
                    case "timestamp":
                        if (builder != null && builder.insideRevision) {
                            String timestamp = getElementText(reader);
                            builder.timestamp = LocalDateTime.parse(timestamp, TIMESTAMP_FORMATTER);
                        }
                        break;
                    case "text":
                        if (builder != null && builder.insideRevision) {
                            builder.text = getElementText(reader);
                        }
                        break;
                }
            }

            if (event.isEndElement()) {
                String elementName = event.asEndElement().getName().getLocalPart();

                if ("page".equals(elementName) && builder != null) {
                    if (builder.isValid()) {
                        WikiPage page = builder.build();
                        if (page != null) {
                            pages.add(page);
                        }
                    }
                    builder = null;
                } else if ("revision".equals(elementName) && builder != null) {
                    builder.insideRevision = false;
                }
            }
        }

        reader.close();
        return pages;
    }

    private String getElementText(XMLEventReader reader) throws XMLStreamException {
        XMLEvent event = reader.nextEvent();
        if (event.isCharacters()) {
            return event.asCharacters().getData();
        }
        return "";
    }

    private static class WikiPageBuilder {
        Long pageId;
        Long revisionId;
        String title;
        Integer namespace;
        String text;
        LocalDateTime timestamp;
        boolean isRedirect = false;
        boolean insideRevision = false;

        boolean isValid() {
            return namespace != null && namespace == 0 && !isRedirect;
        }

        WikiPage build() {
            if (!isValid() || pageId == null || title == null) {
                return null;
            }

            return WikiPage.builder()
                    .pageId(pageId)
                    .revisionId(revisionId)
                    .title(title)
                    .text(text != null ? text : "")
                    .createdDate(timestamp != null ? timestamp : DateTimeUtils.now())
                    .collectedDate(DateTimeUtils.now())
                    .publishedToKafka(false)
                    .build();
        }
    }
}
