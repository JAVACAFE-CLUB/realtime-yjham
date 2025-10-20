package com.realtime.collectionsystem.wiki.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

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

    public void markAsPublished() {
        this.publishedToKafka = true;
    }

    public void updateContent(Long revisionId, String text, LocalDateTime createdDate, LocalDateTime collectedDate) {
        this.revisionId = revisionId;
        this.text = text;
        this.createdDate = createdDate;
        this.collectedDate = collectedDate;
        this.publishedToKafka = false;
    }
}
