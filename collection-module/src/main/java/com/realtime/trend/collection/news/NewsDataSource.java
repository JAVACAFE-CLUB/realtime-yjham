package com.realtime.trend.collection.news;

import com.realtime.trend.collection.core.domain.PublishStatus;
import com.realtime.trend.collection.core.source.DataSource;
import com.realtime.trend.collection.news.batch.NewsItemProcessor;
import com.realtime.trend.collection.news.batch.RssItemReader;
import com.realtime.trend.collection.news.crawler.RssItem;
import com.realtime.trend.collection.news.domain.News;
import com.realtime.trend.collection.news.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 뉴스 데이터 소스 구현체
 */
@Component
@RequiredArgsConstructor
public class NewsDataSource implements DataSource<News> {

    private final NewsRepository newsRepository;
    private final RssItemReader rssItemReader;
    private final NewsItemProcessor newsItemProcessor;

    @Value("${collection.news.enabled:true}")
    private boolean enabled;

    @Value("${collection.news.cron:0 */10 * * * *}")
    private String collectionCron;

    @Value("${collection.news.compensating-cron:0 */30 * * * *}")
    private String compensatingCron;

    @Value("${collection.news.topic:raw-news}")
    private String topicName;

    @Override
    public String getSourceName() {
        return "news";
    }

    @Override
    public String getTopicName() {
        return topicName;
    }

    @Override
    public String getCollectionCron() {
        return collectionCron;
    }

    @Override
    public String getCompensatingCron() {
        return compensatingCron;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public ItemReader<RssItem> createCollectionReader() {
        rssItemReader.reset();  // 상태 초기화
        return rssItemReader;
    }

    @Override
    public ItemProcessor<RssItem, News> createProcessor() {
        return newsItemProcessor;
    }

    @Override
    public List<News> findPendingItems() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        return newsRepository.findByPublishStatusAndCollectedAtBefore(
                PublishStatus.PENDING, threshold);
    }

    @Override
    public News save(News entity) {
        return newsRepository.save(entity);
    }

    @Override
    public Class<News> getEntityType() {
        return News.class;
    }
}
