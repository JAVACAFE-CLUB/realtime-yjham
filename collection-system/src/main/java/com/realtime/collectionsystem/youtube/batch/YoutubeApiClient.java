package com.realtime.collectionsystem.youtube.batch;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import com.realtime.collectionsystem.common.util.DateTimeUtils;
import com.realtime.collectionsystem.youtube.domain.YoutubeVideo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class YoutubeApiClient {

    @Value("${youtube.api.key}")
    private String apiKey;

    private static final String APPLICATION_NAME = "collection-system";
    private static final Long MAX_RESULTS = 10L;

    public List<YoutubeVideo> fetchMostPopularVideos() {
        List<YoutubeVideo> videos = new ArrayList<>();

        try {
            YouTube youtube = new YouTube.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    null
            )
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            YouTube.Videos.List request = youtube.videos()
                    .list(List.of("snippet", "contentDetails", "statistics"))
                    .setChart("mostPopular")
                    .setRegionCode("KR")
                    .setMaxResults(MAX_RESULTS)
                    .setKey(apiKey);

            VideoListResponse response = request.execute();

            for (Video video : response.getItems()) {
                YoutubeVideo youtubeVideo = convertToYoutubeVideo(video);
                videos.add(youtubeVideo);
            }

            log.info("YouTube 인기 동영상 {} 건 수집 완료", videos.size());

        } catch (Exception e) {
            log.error("YouTube API 호출 실패", e);
            throw new RuntimeException("YouTube API 호출 실패", e);
        }

        return videos;
    }

    private YoutubeVideo convertToYoutubeVideo(Video video) {
        LocalDateTime publishedAt = video.getSnippet().getPublishedAt() != null
                ? LocalDateTime.ofInstant(
                Instant.ofEpochMilli(video.getSnippet().getPublishedAt().getValue()),
                ZoneId.of("Asia/Seoul")
        )
                : DateTimeUtils.now();

        return YoutubeVideo.builder()
                .videoId(video.getId())
                .title(video.getSnippet().getTitle())
                .description(video.getSnippet().getDescription())
                .tags(video.getSnippet().getTags())
                .channelTitle(video.getSnippet().getChannelTitle())
                .categoryId(video.getSnippet().getCategoryId())
                .createdDate(publishedAt)
                .collectedDate(DateTimeUtils.now())
                .publishedToKafka(false)
                .build();
    }
}
