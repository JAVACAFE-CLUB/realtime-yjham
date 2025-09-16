package com.realtime.collectionsystem.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Getter
@Builder
@Jacksonized
public class WikiPage {
    private final Long pageId;
    private final String title;
    private final String content;
    private final Integer namespace;
    private final String namespaceName;
    private final Long revisionId;
    private final LocalDateTime lastModified;
    private final String contributor;
    private final Long contributorId;
    private final String editComment;
    private final Integer contentLength;
    private final String sha1Hash;
    private final List<String> categories;
    private final List<String> internalLinks;
    private final List<String> externalLinks;
    private final LocalDateTime collectedDate;

    public WikiPage(Long pageId, String title, String content, Integer namespace, String namespaceName,
                      Long revisionId, LocalDateTime lastModified, String contributor, Long contributorId,
                      String editComment, Integer contentLength, String sha1Hash, List<String> categories,
                      List<String> internalLinks, List<String> externalLinks, LocalDateTime collectedDate) {
        this.pageId = Objects.requireNonNull(pageId, "Page ID cannot be null");
        this.title = Objects.requireNonNull(title, "Title cannot be null");
        this.content = content;
        this.namespace = namespace;
        this.namespaceName = namespaceName;
        this.revisionId = revisionId;
        this.lastModified = lastModified;
        this.contributor = contributor;
        this.contributorId = contributorId;
        this.editComment = editComment;
        this.contentLength = contentLength;
        this.sha1Hash = sha1Hash;
        this.categories = categories != null ? List.copyOf(categories) : List.of();
        this.internalLinks = internalLinks != null ? List.copyOf(internalLinks) : List.of();
        this.externalLinks = externalLinks != null ? List.copyOf(externalLinks) : List.of();
        this.collectedDate = Objects.requireNonNull(collectedDate, "Collected date cannot be null");
    }

    public boolean isMainNamespace() {
        return namespace != null && namespace == 0;
    }

    public boolean isRedirect() {
        return content != null && content.trim().toLowerCase().startsWith("#redirect");
    }

    public boolean isStub() {
        return content != null && content.contains("{{토막글");
    }

    public String getWikipediaUrl() {
        if (title == null) return null;
        return "https://ko.wikipedia.org/wiki/" + title.replace(" ", "_");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WikiPage that = (WikiPage) o;
        return Objects.equals(pageId, that.pageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageId);
    }

    @Override
    public String toString() {
        return "WikiPage{" +
                "pageId=" + pageId +
                ", title='" + title + '\'' +
                ", namespace=" + namespace +
                ", lastModified=" + lastModified +
                '}';
    }
}