package com.realtime.collectionsystem.storage.service;

import com.realtime.collectionsystem.domain.WikiPage;
import com.realtime.collectionsystem.storage.repository.WikiPageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WikiPageStorageService {

    private final WikiPageRepository wikiPageRepository;

    public void saveWikiPages(List<WikiPage> wikiPages) {
        if (wikiPages == null || wikiPages.isEmpty()) {
            log.warn("저장할 위키페이지가 없습니다.");
            return;
        }

        try {
            log.info("위키페이지 저장 시작: {}개", wikiPages.size());
            wikiPageRepository.saveAll(wikiPages);
            log.info("위키페이지 저장 완료: {}개", wikiPages.size());
        } catch (Exception e) {
            log.error("위키페이지 저장 중 오류 발생", e);
            throw e;
        }
    }

    public void saveWikiPage(WikiPage wikiPage) {
        if (wikiPage == null) {
            log.warn("저장할 위키페이지가 null입니다.");
            return;
        }

        try {
            log.debug("위키페이지 저장: ID={}, 제목={}", wikiPage.getPageId(), wikiPage.getTitle());
            wikiPageRepository.save(wikiPage);
            log.debug("위키페이지 저장 완료: ID={}", wikiPage.getPageId());
        } catch (Exception e) {
            log.error("위키페이지 저장 중 오류 발생 - ID: {}, 제목: {}",
                     wikiPage.getPageId(), wikiPage.getTitle(), e);
            throw e;
        }
    }
}