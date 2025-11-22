package com.realtime.trend.collection.news.crawler;

import org.jsoup.nodes.Document;

/**
 * 뉴스 기사 파서 인터페이스 (전략 패턴)
 * 언론사별로 구현체를 제공
 */
public interface NewsArticleParser {

    /**
     * HTML 문서에서 뉴스 기사 정보 추출
     *
     * @param doc Jsoup Document
     * @param url 기사 URL
     * @return 파싱된 뉴스 기사
     */
    NewsArticle parse(Document doc, String url);

    /**
     * 이 파서가 지원하는 언론사명 반환
     */
    String getPublisher();
}
