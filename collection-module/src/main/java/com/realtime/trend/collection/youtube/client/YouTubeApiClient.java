package com.realtime.trend.collection.youtube.client;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import com.realtime.trend.collection.youtube.domain.YouTubeVideo;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    private YouTube youtube;

    public YouTubeApiClient(
            @Value("${youtube.api.key}") String apiKey,
            @Value("${youtube.api.max-results}") long maxResults) {
        this.apiKey = apiKey;
        this.maxResults = maxResults;
    }

    @PostConstruct
    public void init() {
        try {
            this.youtube = new YouTube.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JSON_FACTORY,
                    null)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            log.info("YouTube API 클라이언트 초기화 완료");
        } catch (Exception e) {
            log.error("YouTube API 클라이언트 초기화 실패", e);
            throw new IllegalStateException("YouTube API 클라이언트 초기화 실패", e);
        }
    }

    /**
     * 인기 급상승 동영상 조회
     *
     * @return 동영상 목록 (실패 시 빈 Optional)
     */
    public Optional<List<YouTubeVideo>> fetchTrendingVideos() {
        if (youtube == null) {
            log.error("YouTube API 클라이언트가 초기화되지 않았습니다");
            return Optional.empty();
        }

        if (apiKey == null || apiKey.isBlank()) {
            log.error("YouTube API 키가 설정되지 않았습니다");
            return Optional.empty();
        }

        try {
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

            return Optional.of(convertToYouTubeVideos(videos));

        } catch (GoogleJsonResponseException e) {
            handleGoogleApiError(e);
            return Optional.empty();
        } catch (IOException e) {
            log.error("YouTube API 네트워크 오류", e);
            return Optional.empty();
        } catch (Exception e) {
            log.error("YouTube API 호출 중 예상치 못한 오류", e);
            return Optional.empty();
        }
    }

    /**
     * Google API 오류 처리
     */
    private void handleGoogleApiError(GoogleJsonResponseException e) {
        int statusCode = e.getStatusCode();
        String reason = e.getDetails() != null ? e.getDetails().getMessage() : "Unknown";

        switch (statusCode) {
            case 400 -> log.error("YouTube API 잘못된 요청: {}", reason);
            case 401, 403 -> log.error("YouTube API 인증 오류 (API 키 확인 필요): {}", reason);
            case 404 -> log.error("YouTube API 리소스를 찾을 수 없음: {}", reason);
            case 429 -> log.error("YouTube API 쿼터 초과: {}", reason);
            default -> log.error("YouTube API 오류 ({}): {}", statusCode, reason);
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
