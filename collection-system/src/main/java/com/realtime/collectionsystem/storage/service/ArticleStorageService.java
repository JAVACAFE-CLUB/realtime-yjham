package com.realtime.collectionsystem.storage.service;

import com.realtime.collectionsystem.domain.Article;
import com.realtime.collectionsystem.storage.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleStorageService {

    private final ArticleRepository articleRepository;

    public void saveArticles(List<Article> articles) {
        if (articles == null || articles.isEmpty()) {
            log.warn("저장할 기사가 없습니다.");
            return;
        }

        try {
            log.info("기사 저장 시작: {}개", articles.size());
            articleRepository.saveAll(articles);
            log.info("기사 저장 완료: {}개", articles.size());
        } catch (Exception e) {
            log.error("기사 저장 중 오류 발생", e);
            throw e;
        }
    }

    public void saveArticle(Article article) {
        if (article == null) {
            log.warn("저장할 기사가 null입니다.");
            return;
        }

        try {
            log.debug("단일 기사 저장: {}", article.getTitle());
            articleRepository.save(article);
            log.debug("단일 기사 저장 완료");
        } catch (Exception e) {
            log.error("단일 기사 저장 중 오류 발생: {}", article.getUrl(), e);
            throw e;
        }
    }
}