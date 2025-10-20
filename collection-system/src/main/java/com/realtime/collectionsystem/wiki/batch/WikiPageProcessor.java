package com.realtime.collectionsystem.wiki.batch;

import com.realtime.collectionsystem.common.util.DateTimeUtils;
import com.realtime.collectionsystem.wiki.domain.WikiPage;
import com.realtime.collectionsystem.wiki.repository.WikiPageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class WikiPageProcessor implements ItemProcessor<WikiPage, WikiPage> {

    private final WikiPageRepository repository;

    @Override
    public WikiPage process(WikiPage item) {
        Optional<WikiPage> existing = repository.findByTitle(item.getTitle());

        if (existing.isEmpty()) {
            return item;
        }

        WikiPage existingPage = existing.get();

        // revisionId가 다른 경우에만 업데이트
        if (!existingPage.getRevisionId().equals(item.getRevisionId())) {
            existingPage.updateContent(
                    item.getRevisionId(),
                    item.getText(),
                    item.getCreatedDate(),
                    DateTimeUtils.now()
            );
            log.debug("위키피디아 페이지 업데이트: {}", item.getTitle());
            return existingPage;
        }

        // 같은 revisionId면 스킵
        log.debug("위키피디아 페이지 스킵 (동일 revisionId): {}", item.getTitle());
        return null;
    }
}
