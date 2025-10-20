package com.realtime.collectionsystem.news.batch.crawler;

import com.realtime.collectionsystem.common.util.DateTimeUtils;
import com.realtime.collectionsystem.news.domain.NewsArticle;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class DongaNewsCrawler implements NewsCrawler {

    private static final String SOURCE = "donga";
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Override
    public String getSource() {
        return SOURCE;
    }

    @Override
    public NewsArticle crawl(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .get();

            // 제목 추출 - 다양한 셀렉터 시도
            String title = "";
            if (doc.selectFirst(".article_title h1") != null) {
                title = doc.selectFirst(".article_title h1").text();
            } else if (doc.selectFirst("h1.title") != null) {
                title = doc.selectFirst("h1.title").text();
            } else if (doc.selectFirst("h1") != null) {
                title = doc.selectFirst("h1").text();
            }

            // 본문 추출 - 다양한 셀렉터 시도
            Elements paragraphs = doc.select(".article_txt p");
            if (paragraphs.isEmpty()) {
                paragraphs = doc.select(".article_body p");
            }
            if (paragraphs.isEmpty()) {
                paragraphs = doc.select("#article_body p");
            }
            
            StringBuilder textBuilder = new StringBuilder();
            for (Element p : paragraphs) {
                String pText = p.text();
                // 불필요한 텍스트 필터링
                if (!pText.contains("무단전재") && 
                    !pText.contains("재배포 금지") &&
                    pText.length() > 10) {
                    textBuilder.append(pText).append("\n");
                }
            }
            String text = textBuilder.toString().trim();

            // 카테고리 추출
            String category = "";
            Element categoryElement = doc.selectFirst(".location");
            if (categoryElement == null) {
                categoryElement = doc.selectFirst(".category");
            }
            if (categoryElement != null) {
                category = categoryElement.text();
            }

            // 작성일 추출
            Element metaDate = doc.selectFirst("meta[property=dd:published_time]");
            if (metaDate == null) {
                metaDate = doc.selectFirst("meta[property=article:published_time]");
            }
            String dateString = metaDate != null ? metaDate.attr("content") : null;

            return NewsArticle.builder()
                    .source(SOURCE)
                    .title(title)
                    .text(text)
                    .url(url)
                    .category(category)
                    .createdDate(dateString != null
                            ? DateTimeUtils.parseToLocalDateTime(dateString, DATE_FORMATTER)
                            : DateTimeUtils.now())
                    .collectedDate(DateTimeUtils.now())
                    .publishedToKafka(false)
                    .build();

        } catch (Exception e) {
            log.error("동아일보 크롤링 실패: {}", url, e);
            throw new RuntimeException("동아일보 크롤링 실패", e);
        }
    }
}
