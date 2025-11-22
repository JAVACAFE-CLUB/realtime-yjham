package com.realtime.trend.collection.youtube.batch;

import com.realtime.trend.collection.youtube.client.YouTubeApiClient;
import com.realtime.trend.collection.youtube.domain.YouTubeVideo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("YouTubeVideoReader 단위 테스트")
@ExtendWith(MockitoExtension.class)
class YouTubeVideoReaderTest {

    @Mock
    private YouTubeApiClient apiClient;

    private YouTubeVideoReader reader;

    @BeforeEach
    void setUp() {
        reader = new YouTubeVideoReader(apiClient);
    }

    @Nested
    @DisplayName("read() 메서드")
    class ReadMethod {

        @Test
        @DisplayName("API에서 가져온 동영상을 순차적으로 반환해야 한다")
        void read_shouldReturnVideosSequentially() {
            // given
            YouTubeVideo video1 = createTestVideo("video-1");
            YouTubeVideo video2 = createTestVideo("video-2");
            YouTubeVideo video3 = createTestVideo("video-3");

            when(apiClient.fetchTrendingVideos())
                    .thenReturn(Optional.of(List.of(video1, video2, video3)));

            // when & then
            assertThat(reader.read()).isEqualTo(video1);
            assertThat(reader.read()).isEqualTo(video2);
            assertThat(reader.read()).isEqualTo(video3);
            assertThat(reader.read()).isNull();
        }

        @Test
        @DisplayName("API 호출은 첫 read()에서만 수행해야 한다")
        void read_shouldCallApiOnlyOnce() {
            // given
            YouTubeVideo video = createTestVideo("video-1");
            when(apiClient.fetchTrendingVideos())
                    .thenReturn(Optional.of(List.of(video)));

            // when
            reader.read();
            reader.read();
            reader.read();

            // then
            verify(apiClient, times(1)).fetchTrendingVideos();
        }

        @Test
        @DisplayName("API 호출 실패 시 빈 목록으로 처리해야 한다")
        void read_withApiFailure_shouldReturnNull() {
            // given
            when(apiClient.fetchTrendingVideos()).thenReturn(Optional.empty());

            // when & then
            assertThat(reader.read()).isNull();
        }

        @Test
        @DisplayName("동영상이 없으면 null을 반환해야 한다")
        void read_withEmptyList_shouldReturnNull() {
            // given
            when(apiClient.fetchTrendingVideos()).thenReturn(Optional.of(List.of()));

            // when & then
            assertThat(reader.read()).isNull();
        }
    }

    @Nested
    @DisplayName("reset() 메서드")
    class ResetMethod {

        @Test
        @DisplayName("reset() 후 다시 API를 호출해야 한다")
        void reset_shouldAllowReinitialization() {
            // given
            YouTubeVideo video1 = createTestVideo("video-1");
            YouTubeVideo video2 = createTestVideo("video-2");

            when(apiClient.fetchTrendingVideos())
                    .thenReturn(Optional.of(List.of(video1)))
                    .thenReturn(Optional.of(List.of(video2)));

            // 첫 번째 읽기
            assertThat(reader.read()).isEqualTo(video1);
            assertThat(reader.read()).isNull();

            // when
            reader.reset();

            // then
            assertThat(reader.read()).isEqualTo(video2);
            verify(apiClient, times(2)).fetchTrendingVideos();
        }

        @Test
        @DisplayName("reset() 후 read()는 처음부터 다시 시작해야 한다")
        void reset_shouldClearState() {
            // given
            YouTubeVideo video = createTestVideo("video-1");
            when(apiClient.fetchTrendingVideos())
                    .thenReturn(Optional.of(List.of(video)));

            reader.read(); // 초기화 및 첫 번째 읽기

            // when
            reader.reset();

            // then - reset 후에는 아직 초기화되지 않은 상태
            // 다음 read()에서 다시 초기화됨
            assertThat(reader.read()).isEqualTo(video);
        }
    }

    @Nested
    @DisplayName("상태 관리")
    class StateManagement {

        @Test
        @DisplayName("여러 번 read() 호출 후 모두 소진되면 null 반환")
        void read_afterExhausted_shouldReturnNull() {
            // given
            YouTubeVideo video = createTestVideo("video-1");
            when(apiClient.fetchTrendingVideos())
                    .thenReturn(Optional.of(List.of(video)));

            // when
            reader.read(); // video 반환
            
            // then
            assertThat(reader.read()).isNull();
            assertThat(reader.read()).isNull(); // 계속 null
        }
    }

    private YouTubeVideo createTestVideo(String videoId) {
        return YouTubeVideo.builder()
                .videoId(videoId)
                .title("테스트 영상 " + videoId)
                .description("테스트 설명")
                .channelTitle("테스트 채널")
                .publishedAt(LocalDateTime.now())
                .viewCount(1000L)
                .likeCount(100L)
                .commentCount(50L)
                .collectedAt(LocalDateTime.now())
                .build();
    }
}
