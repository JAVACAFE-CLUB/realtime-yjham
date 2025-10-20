package com.realtime.collectionsystem.wiki.batch;

import com.realtime.collectionsystem.wiki.domain.WikiPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WikiPageReader implements ItemReader<WikiPage> {

    private final WikiXmlParser xmlParser;
    private Iterator<WikiPage> pages;

    @Override
    public WikiPage read() throws Exception {
        if (pages == null) {
            pages = fetchAllPages().iterator();
        }

        if (pages.hasNext()) {
            return pages.next();
        }

        return null;
    }

    private List<WikiPage> fetchAllPages() {
        List<WikiPage> allPages = new ArrayList<>();

        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:wiki/*.xml");

            for (Resource resource : resources) {
                log.info("위키피디아 XML 파일 파싱 시작: {}", resource.getFilename());

                try (InputStream inputStream = resource.getInputStream()) {
                    List<WikiPage> pages = xmlParser.parse(inputStream);
                    allPages.addAll(pages);
                    log.info("위키피디아 {} 건 파싱 완료", pages.size());
                } catch (Exception e) {
                    log.error("위키피디아 XML 파싱 실패: {}", resource.getFilename(), e);
                }
            }

            log.info("전체 위키피디아 페이지 {} 건 수집", allPages.size());

        } catch (Exception e) {
            log.error("위키피디아 파일 로드 실패", e);
        }

        return allPages;
    }
}
