package com.realtime.cleansingsystem.wiki.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 정제된 위키피디아 데이터
 * cleansing database에 저장
 */
@Document(collection = "cleansed_wiki")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CleansedWiki {

    @Id
    private String id;  // 원본 WikiPage의 id와 동일하게 유지

    private Long pageId;

    private Long revisionId;

    @Indexed
    private String title;

    private String cleanedText;  // 위키 마크업 제거된 텍스트

    private LocalDateTime createdDate;

    private LocalDateTime collectedDate;

    @Indexed
    private LocalDateTime cleansedDate;

    public static CleansedWiki from(WikiPage page, String cleanedText) {
        return CleansedWiki.builder()
                .id(page.getId())
                .pageId(page.getPageId())
                .revisionId(page.getRevisionId())
                .title(page.getTitle())
                .cleanedText(cleanedText)
                .createdDate(page.getCreatedDate())
                .collectedDate(page.getCollectedDate())
                .cleansedDate(LocalDateTime.now())
                .build();
    }
}
