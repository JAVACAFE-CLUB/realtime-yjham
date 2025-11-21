package com.realtime.trend.collection.service.youtube;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import com.realtime.trend.collection.domain.YouTubeVideo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * YouTube Data API v3 클라이언트
 * 한국 인기 급상승 동영상 수집
 */
@Slf4j
@Component
public class YouTubeApiClient {

    private static final String APPLICATION_NAME = "Realtime-Trend-Collection";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String REGION_CODE = "KR"; // 대한민국

    private final String apiKey;
    private final long maxResults;

    public YouTubeApiClient(
            @Value("${youtube.api.key}") String apiKey,
            @Value("${youtube.api.max-results}") long maxResults) {
        this.apiKey = apiKey;
        this.maxResults = maxResults;
    }

    /**
     * 인기 급상승 동영상 조회
     *
     * @return 동영상 목록
     */
    public List<YouTubeVideo> fetchTrendingVideos() {
        try {
            YouTube youtube = new YouTube.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JSON_FACTORY,
                    null)
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            // 인기 급상승 동영상 요청
            YouTube.Videos.List request = youtube.videos()
                    .list(List.of("snippet", "statistics"))
                    .setChart("mostPopular")
                    .setRegionCode(REGION_CODE)
                    .setMaxResults(maxResults)
                    .setKey(apiKey);

            VideoListResponse response = request.execute();
            List<Video> videos = response.getItems();

            log.info("YouTube API 호출 성공: {}개 동영상 수집", videos.size());

            return convertToYouTubeVideos(videos);

        } catch (Exception e) {
            log.error("YouTube API 호출 실패", e);
            return List.of();
        }
    }

    /**
     * YouTube API 응답을 도메인 객체로 변환
     */
    private List<YouTubeVideo> convertToYouTubeVideos(List<Video> videos) {
        List<YouTubeVideo> result = new ArrayList<>();
        LocalDateTime collectedAt = LocalDateTime.now();

        for (Video video : videos) {
            try {
                YouTubeVideo youTubeVideo = YouTubeVideo.builder()
                        .videoId(video.getId())
                        .title(video.getSnippet().getTitle())
                        .description(video.getSnippet().getDescription())
                        .channelTitle(video.getSnippet().getChannelTitle())
                        .publishedAt(convertToLocalDateTime(video.getSnippet().getPublishedAt()))
                        .categoryId(video.getSnippet().getCategoryId())
                        .tags(video.getSnippet().getTags())
                        .viewCount(video.getStatistics().getViewCount().longValue())
                        .likeCount(video.getStatistics().getLikeCount() != null
                                ? video.getStatistics().getLikeCount().longValue()
                                : 0L)
                        .commentCount(video.getStatistics().getCommentCount() != null
                                ? video.getStatistics().getCommentCount().longValue()
                                : 0L)
                        .collectedAt(collectedAt)
                        .build();

                result.add(youTubeVideo);

            } catch (Exception e) {
                log.warn("동영상 변환 실패: {}", video.getId(), e);
            }
        }

        return result;
    }

    /**
     * YouTube DateTime을 LocalDateTime으로 변환
     */
    private LocalDateTime convertToLocalDateTime(com.google.api.client.util.DateTime dateTime) {
        if (dateTime == null) {
            return LocalDateTime.now();
        }
        return Instant.ofEpochMilli(dateTime.getValue())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}
