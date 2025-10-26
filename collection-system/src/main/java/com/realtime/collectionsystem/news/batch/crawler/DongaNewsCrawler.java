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

            // 제목 추출 - h2.sub_tit 사용
            String title = "";
            Element titleElement = doc.selectFirst("h2.sub_tit");
            if (titleElement != null) {
                title = titleElement.text();
            }
            if (title.isEmpty() && doc.selectFirst("title") != null) {
                // fallback: title 태그에서 추출 (｜동아일보 제거)
                title = doc.selectFirst("title").text().replace("｜동아일보", "").trim();
            }

            // 본문 추출 - .news_view section 내의 직접 텍스트 노드
            Element newsView = doc.selectFirst("section.news_view");
            String text = "";
            
            if (newsView != null) {
                // HTML에서 광고, 스크립트, 이미지 등 불필요한 요소 제거
                newsView.select("script, style, figure, .view_ad06, .view_m_adA, .view_m_adK").remove();
                
                // 텍스트 추출 (개행 유지)
                String rawText = newsView.html();
                // br 태그를 개행으로 변환하고 HTML 태그 제거
                rawText = rawText.replaceAll("<br[^>]*>", "\n");
                Document tempDoc = Jsoup.parse(rawText);
                text = tempDoc.text();
                
                // 불필요한 텍스트 필터링
                StringBuilder textBuilder = new StringBuilder();
                for (String line : text.split("\n")) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty() && 
                        !trimmed.contains("무단전재") && 
                        !trimmed.contains("재배포 금지") &&
                        !trimmed.contains("BYLINE") &&
                        trimmed.length() > 10) {
                        textBuilder.append(trimmed).append("\n");
                    }
                }
                text = textBuilder.toString().trim();
            }

            // 카테고리 추출 - meta 태그에서 추출
            String category = "";
            Element categoryMeta = doc.selectFirst("meta[name=categoryname]");
            if (categoryMeta != null) {
                String categoryContent = categoryMeta.attr("content");
                // 콤마로 구분된 카테고리 중 첫 번째만 사용
                if (categoryContent != null && !categoryContent.isEmpty()) {
                    category = categoryContent.split(",")[0].trim();
                }
            }

            // 작성일 추출
            Element metaDate = doc.selectFirst("meta[property=dd:published_time]");
            if (metaDate == null) {
                metaDate = doc.selectFirst("meta[property=article:published_time]");
            }
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
            log.error("동아일보 크롤링 실패: {}", url, e);
            throw new RuntimeException("동아일보 크롤링 실패", e);
        }
    }
}
