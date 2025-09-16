package com.realtime.collectionsystem.storage.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.realtime.collectionsystem.domain.WikiPage;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WikiPageSerializationTest {

    @Test
    void testWikiPageSerialization() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        WikiPage wikiPage = WikiPage.builder()
                .pageId(10112L)
                .title("분류:물리학자")
                .content("test content")
                .namespace(14)
                .namespaceName("분류")
                .revisionId(38930101L)
                .lastModified(LocalDateTime.of(2025, 3, 19, 0, 11, 5))
                .contributor("A.TedBot")
                .contributorId(414775L)
                .editComment("test comment")
                .contentLength(68)
                .sha1Hash("r136p143jqkuar6vxo6tf39im8njflq")
                .categories(List.of("분야별 과학자", "물리학"))
                .internalLinks(List.of())
                .externalLinks(List.of())
                .collectedDate(LocalDateTime.now())
                .build();

        String json = objectMapper.writeValueAsString(wikiPage);
        System.out.println("Serialized JSON length: " + json.length());
        System.out.println("JSON content: " + json);

        // JSON이 완전한지 확인
        assertTrue(json.contains("collectedDate"));
        assertTrue(json.endsWith("}"));
        assertTrue(json.length() > 200); // 최소 길이 확인
    }
}