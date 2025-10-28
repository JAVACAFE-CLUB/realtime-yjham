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
 * 위키피디아 원본 데이터
 * collection-system에서 저장한 데이터 조회용
 */
@Document(collection = "wiki_pages")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WikiPage {

    @Id
    private String id;

    private Long pageId;

    @Indexed
    private Long revisionId;

    @Indexed(unique = true)
    private String title;

    private String text;

    private LocalDateTime createdDate;

    @Indexed
    private LocalDateTime collectedDate;

    @Indexed
    @Builder.Default
    private Boolean publishedToKafka = false;
}
