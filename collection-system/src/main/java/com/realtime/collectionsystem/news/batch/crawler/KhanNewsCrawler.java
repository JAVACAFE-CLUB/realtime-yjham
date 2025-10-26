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

            // 제목 추출 - h1 태그 사용 (기사 본문 영역 내)
            String title = "";
            Element artCont = doc.selectFirst("section.art_cont");
            if (artCont != null) {
                Element h1 = artCont.selectFirst("h1");
                if (h1 != null) {
                    title = h1.text();
                }
            }
            
            // fallback: header 영역의 .article-title
            if (title.isEmpty()) {
                Element headerTitle = doc.selectFirst(".article-title");
                if (headerTitle != null) {
                    title = headerTitle.text();
                }
            }

            // 본문 추출 - #articleBody 내의 p.content_text
            Elements paragraphs = doc.select("#articleBody p.content_text");
            if (paragraphs.isEmpty()) {
                // fallback: .art_body 내의 모든 p 태그
                paragraphs = doc.select(".art_body p");
            }
            
            StringBuilder textBuilder = new StringBuilder();
            for (Element p : paragraphs) {
                String pText = p.text();
                // 불필요한 텍스트 필터링
                if (!pText.contains("경향신문 메뉴") && 
                    !pText.contains("facebook") && 
                    !pText.contains("youtube") &&
                    !pText.contains("twitter") &&
                    !pText.isEmpty() &&
                    pText.length() > 10) {
                    textBuilder.append(pText).append("\n");
                }
            }
            String text = textBuilder.toString().trim();

            // 카테고리 추출 - meta 태그에서 추출
            String category = "";
            Element categoryMeta = doc.selectFirst("meta[property=article:section]");
            if (categoryMeta != null) {
                category = categoryMeta.attr("content");
            }
            // fallback: og:category 사용
            if (category == null || category.isEmpty()) {
                Element ogCategory = doc.selectFirst("meta[property=og:category]");
                if (ogCategory != null) {
                    category = ogCategory.attr("content");
                }
            }

            // 작성일 추출
            Element metaDate = doc.selectFirst("meta[property=article:published_time]");
            if (metaDate == null) {
                metaDate = doc.selectFirst("meta[property=og:pubdate]");
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
            log.error("경향신문 크롤링 실패: {}", url, e);
            throw new RuntimeException("경향신문 크롤링 실패", e);
        }
    }
}
