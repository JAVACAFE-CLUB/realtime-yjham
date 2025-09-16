package com.realtime.collectionsystem.storage.repository;

import com.realtime.collectionsystem.domain.WikiPage;

import java.util.List;

public interface WikiPageRepository {

    void save(WikiPage wikiPage);

    void saveAll(List<WikiPage> wikiPages);
}