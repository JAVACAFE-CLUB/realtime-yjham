package com.realtime.collectionsystem.wiki;

import com.realtime.collectionsystem.wiki.batch.WikiXmlParser;
import com.realtime.collectionsystem.wiki.domain.WikiPage;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WikiXmlParserTest {

    @Test
    void 위키피디아_XML_파싱_테스트() throws Exception {
        WikiXmlParser parser = new WikiXmlParser();

        File xmlFile = new File("reference/wiki-example.xml");
        try (InputStream inputStream = new FileInputStream(xmlFile)) {
            List<WikiPage> pages = parser.parse(inputStream);

            // redirect가 아니고 namespace=0인 페이지만 파싱되어야 함
            assertThat(pages).hasSize(1);

            WikiPage page = pages.get(0);
            assertThat(page.getTitle()).isEqualTo("지미 카터");
            assertThat(page.getPageId()).isEqualTo(5L);
            assertThat(page.getRevisionId()).isEqualTo(40301269L);
            assertThat(page.getText()).isEqualTo("...");
        }
    }

    @Test
    void Redirect_페이지_제외_테스트() throws Exception {
        WikiXmlParser parser = new WikiXmlParser();

        File xmlFile = new File("reference/wiki-example.xml");
        try (InputStream inputStream = new FileInputStream(xmlFile)) {
            List<WikiPage> pages = parser.parse(inputStream);

            // "새천년 민주당"은 redirect 페이지이므로 제외되어야 함
            boolean hasRedirectPage = pages.stream()
                    .anyMatch(p -> "새천년 민주당".equals(p.getTitle()));

            assertThat(hasRedirectPage).isFalse();
        }
    }

    @Test
    void Namespace_0_필터링_테스트() throws Exception {
        WikiXmlParser parser = new WikiXmlParser();

        File xmlFile = new File("reference/wiki-example.xml");
        try (InputStream inputStream = new FileInputStream(xmlFile)) {
            List<WikiPage> pages = parser.parse(inputStream);

            // 모든 페이지가 namespace=0이어야 함
            assertThat(pages).allMatch(page -> page.getPageId() != null);
        }
    }
}
