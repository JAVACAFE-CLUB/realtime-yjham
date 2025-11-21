package com.realtime.trend.collection.service.crawler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTML 크롤러
 * 전략 패턴으로 언론사별 파서를 관리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HtmlCrawler {

    private final List<NewsArticleParser> parsers;
    private Map<String, NewsArticleParser> parserMap;

    /**
     * 언론사별 파서 맵 초기화
     */
    private void initializeParserMap() {
        if (parserMap == null) {
            parserMap = new HashMap<>();
            for (NewsArticleParser parser : parsers) {
                parserMap.put(parser.getPublisher(), parser);
            }
            log.info("파서 초기화 완료: {}개", parserMap.size());
        }
    }

    /**
     * URL에서 기사 크롤링
     *
     * @param url       기사 URL
     * @param publisher 언론사명
     * @return 크롤링된 기사 정보
     */
    public NewsArticle crawl(String url, String publisher) {
        initializeParserMap();

        NewsArticleParser parser = parserMap.get(publisher);
        if (parser == null) {
            log.error("지원하지 않는 언론사: {}", publisher);
            return null;
        }

        try {
            // HTML 가져오기
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .get();

            // 파싱
            NewsArticle article = parser.parse(doc, url);

            if (article != null) {
                log.debug("크롤링 성공: {} - {}", publisher, url);
            } else {
                log.warn("파싱 실패: {} - {}", publisher, url);
            }

            return article;

        } catch (Exception e) {
            log.error("크롤링 실패: {} - {}", publisher, url, e);
            return null;
        }
    }
}
