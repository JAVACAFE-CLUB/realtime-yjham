package com.realtime.collectionsystem.news;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class RssFeedReaderTest {

    @Test
    void 경향신문_RSS_Feed_파싱_테스트() throws IOException {
        File xmlFile = new File("reference/kyunghyang-rss.xml");
        Document doc = Jsoup.parse(xmlFile, "UTF-8", "", org.jsoup.parser.Parser.xmlParser());

        Elements items = doc.select("item");
        assertThat(items).isNotEmpty();

        Element firstItem = items.first();
        assertThat(firstItem).isNotNull();

        String url = firstItem.selectFirst("link") != null
                ? firstItem.selectFirst("link").text()
                : "";
        assertThat(url).isNotBlank();
    }

    @Test
    void 동아일보_RSS_Feed_파싱_테스트() throws IOException {
        File xmlFile = new File("reference/donga-rss.xml");
        Document doc = Jsoup.parse(xmlFile, "UTF-8", "", org.jsoup.parser.Parser.xmlParser());

        Elements items = doc.select("item");
        assertThat(items).isNotEmpty();

        Element firstItem = items.first();
        assertThat(firstItem).isNotNull();

        String url = firstItem.selectFirst("link") != null
                ? firstItem.selectFirst("link").text()
                : "";
        assertThat(url).isNotBlank();
    }
}
