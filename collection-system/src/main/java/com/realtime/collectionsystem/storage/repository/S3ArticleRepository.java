package com.realtime.collectionsystem.storage.repository;

import com.realtime.collectionsystem.domain.Article;

import java.util.List;

public class S3ArticleRepository implements ArticleRepository {

    @Override
    public void save(Article article) {
        // TODO: S3 저장 로직 구현 예정
        throw new UnsupportedOperationException("S3 저장 기능은 아직 구현되지 않았습니다.");
    }

    @Override
    public void saveAll(List<Article> articles) {
        // TODO: S3 일괄 저장 로직 구현 예정
        throw new UnsupportedOperationException("S3 일괄 저장 기능은 아직 구현되지 않았습니다.");
    }
}
