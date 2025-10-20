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
public class KhanNewsCrawler implements NewsCrawler {

    private static final String SOURCE = "khan";
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

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
            if (doc.selectFirst(".art_header h1") != null) {
                title = doc.selectFirst(".art_header h1").text();
            } else if (doc.selectFirst("h1.headline") != null) {
                title = doc.selectFirst("h1.headline").text();
            } else if (doc.selectFirst(".headline") != null) {
                title = doc.selectFirst(".headline").text();
            }

            // 본문 추출 - 다양한 셀렉터 시도
            Elements paragraphs = doc.select(".art_body p");
            if (paragraphs.isEmpty()) {
                paragraphs = doc.select("#articeBody p");
            }
            if (paragraphs.isEmpty()) {
                paragraphs = doc.select("#articleBody p");
            }
            
            StringBuilder textBuilder = new StringBuilder();
            for (Element p : paragraphs) {
                String pText = p.text();
                // 메뉴 텍스트나 불필요한 텍스트 필터링
                if (!pText.contains("경향신문 메뉴") && 
                    !pText.contains("facebook") && 
                    !pText.contains("youtube") &&
                    pText.length() > 10) {
                    textBuilder.append(pText).append("\n");
                }
            }
            String text = textBuilder.toString().trim();

            // 카테고리 추출
            String category = "";
            Element categoryElement = doc.selectFirst(".sec_menu");
            if (categoryElement == null) {
                categoryElement = doc.selectFirst(".category");
            }
            if (categoryElement != null) {
                category = categoryElement.text();
            }

            // 작성일 추출
            Element metaDate = doc.selectFirst("meta[property=article:published_time]");
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
            log.error("경향신문 크롤링 실패: {}", url, e);
            throw new RuntimeException("경향신문 크롤링 실패", e);
        }
    }
}
