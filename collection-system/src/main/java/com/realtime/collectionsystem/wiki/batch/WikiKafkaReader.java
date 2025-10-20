package com.realtime.collectionsystem.wiki.batch;

import com.realtime.collectionsystem.wiki.domain.WikiPage;
import com.realtime.collectionsystem.wiki.repository.WikiPageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class WikiKafkaReader implements ItemReader<WikiPage> {

    private final WikiPageRepository repository;
    private Iterator<WikiPage> pages;

    @Override
    public WikiPage read() {
        if (pages == null) {
            List<WikiPage> unpublished = repository.findByPublishedToKafkaFalse();
            pages = unpublished.iterator();
        }

        if (pages.hasNext()) {
            return pages.next();
        }

        return null;
    }
}
