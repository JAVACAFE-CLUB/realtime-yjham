package com.realtime.trend.collection.youtube.batch;

import com.realtime.trend.collection.youtube.domain.YouTubeVideo;
import com.realtime.trend.collection.youtube.repository.YouTubeVideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("YouTubeVideoProcessor 단위 테스트")
@ExtendWith(MockitoExtension.class)
class YouTubeVideoProcessorTest {

    @Mock
    private YouTubeVideoRepository youTubeVideoRepository;

    @InjectMocks
    private YouTubeVideoProcessor processor;

    private YouTubeVideo testVideo;

    @BeforeEach
    void setUp() {
        testVideo = YouTubeVideo.builder()
                .videoId("abc123xyz")
                .title("테스트 유튜브 영상")
                .description("테스트 설명입니다.")
                .channelTitle("테스트 채널")
                .publishedAt(LocalDateTime.now())
                .categoryId("22")
                .tags(List.of("테스트", "영상"))
                .viewCount(1000L)
                .likeCount(50L)
                .commentCount(10L)
                .collectedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("process() 메서드")
    class ProcessMethod {

        @Test
        @DisplayName("새로운 동영상은 그대로 반환해야 한다")
        void process_withNewVideo_shouldReturnVideo() throws Exception {
            // given
            when(youTubeVideoRepository.existsByVideoId(anyString())).thenReturn(false);

            // when
            YouTubeVideo result = processor.process(testVideo);

            // then
            assertThat(result).isNotNull();
            assertThat(result).isSameAs(testVideo);
            verify(youTubeVideoRepository).existsByVideoId(testVideo.getVideoId());
        }

        @Test
        @DisplayName("이미 수집된 동영상은 null을 반환해야 한다 (중복 체크)")
        void process_withDuplicateVideoId_shouldReturnNull() throws Exception {
            // given
            when(youTubeVideoRepository.existsByVideoId(anyString())).thenReturn(true);

            // when
            YouTubeVideo result = processor.process(testVideo);

            // then
            assertThat(result).isNull();
            verify(youTubeVideoRepository).existsByVideoId(testVideo.getVideoId());
        }
    }
}
