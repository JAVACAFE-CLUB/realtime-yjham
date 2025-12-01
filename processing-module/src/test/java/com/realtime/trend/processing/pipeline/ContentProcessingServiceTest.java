package com.realtime.trend.processing.pipeline;

import com.realtime.trend.processing.dto.ExtractedEntity;
import com.realtime.trend.processing.dto.ProcessedNewsMessage;
import com.realtime.trend.processing.dto.ProcessedYoutubeMessage;
import com.realtime.trend.processing.dto.RawNewsMessage;
import com.realtime.trend.processing.dto.RawYoutubeMessage;
import com.realtime.trend.processing.extraction.NerClient;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContentProcessingService 단위 테스트")
class ContentProcessingServiceTest {

    @Mock
    private TextProcessor textProcessor;

    @Mock
    private ContentValidator contentValidator;

    @Mock
    private NerClient nerClient;

    private ContentProcessingService service;

    @BeforeEach
    void setUp() {
        service = new ContentProcessingService(textProcessor, contentValidator, nerClient);
    }

    @Nested
    @DisplayName("processNews 메서드")
    class ProcessNewsMethod {

        private RawNewsMessage createRawNewsMessage() {
            return new RawNewsMessage(
                    "news-001",
                    "https://example.com/news/1",
                    "테스트 뉴스 제목",
                    "<p>테스트 뉴스 본문 내용입니다.</p>",
                    LocalDateTime.now().minusHours(1),
                    "테스트신문",
                    "홍길동",
                    "경제",
                    List.of("금리", "한국은행"),
                    LocalDateTime.now()
            );
        }

        @Test
        @DisplayName("정상적으로 뉴스를 처리해야 한다")
        void shouldProcessNewsSuccessfully() {
            // given
            RawNewsMessage raw = createRawNewsMessage();
            String processedContent = "테스트 뉴스 본문 내용입니다.";
            List<ExtractedEntity> keywords = List.of(
                    new ExtractedEntity("한국은행", "ORGANIZATION"),
                    new ExtractedEntity("금리", "TERM")
            );

            when(textProcessor.process(raw.content())).thenReturn(processedContent);
            when(contentValidator.validate(processedContent))
                    .thenReturn(ContentValidator.ValidationResult.success());
            when(nerClient.analyze(anyString())).thenReturn(keywords);

            // when
            Optional<ProcessedNewsMessage> result = service.processNews(raw);

            // then
            assertThat(result).isPresent();
            ProcessedNewsMessage processed = result.get();
            assertThat(processed.id()).isEqualTo(raw.id());
            assertThat(processed.url()).isEqualTo(raw.url());
            assertThat(processed.title()).isEqualTo(raw.title());
            assertThat(processed.processedContent()).isEqualTo(processedContent);
            assertThat(processed.keywords()).hasSize(2);
            assertThat(processed.keywords()).containsExactlyInAnyOrderElementsOf(keywords);
        }

        @Test
        @DisplayName("검증 실패 시 빈 Optional을 반환해야 한다")
        void shouldReturnEmptyWhenValidationFails() {
            // given
            RawNewsMessage raw = createRawNewsMessage();
            String processedContent = "짧은 내용";

            when(textProcessor.process(raw.content())).thenReturn(processedContent);
            when(contentValidator.validate(processedContent))
                    .thenReturn(ContentValidator.ValidationResult.failure("길이 부족"));

            // when
            Optional<ProcessedNewsMessage> result = service.processNews(raw);

            // then
            assertThat(result).isEmpty();
            verify(nerClient, never()).analyze(anyString());
        }

        @Test
        @DisplayName("TextProcessor를 호출해야 한다")
        void shouldCallTextProcessor() {
            // given
            RawNewsMessage raw = createRawNewsMessage();
            when(textProcessor.process(anyString())).thenReturn("처리된 내용");
            when(contentValidator.validate(anyString()))
                    .thenReturn(ContentValidator.ValidationResult.success());
            when(nerClient.analyze(anyString())).thenReturn(List.of());

            // when
            service.processNews(raw);

            // then
            verify(textProcessor).process(raw.content());
        }

        @Test
        @DisplayName("NER 분석 시 제목과 본문을 결합해야 한다")
        void shouldCombineTitleAndContentForNer() {
            // given
            RawNewsMessage raw = createRawNewsMessage();
            String processedContent = "처리된 본문";

            when(textProcessor.process(raw.content())).thenReturn(processedContent);
            when(contentValidator.validate(processedContent))
                    .thenReturn(ContentValidator.ValidationResult.success());
            when(nerClient.analyze(anyString())).thenReturn(List.of());

            // when
            service.processNews(raw);

            // then
            verify(nerClient).analyze(raw.title() + " " + processedContent);
        }

        @Test
        @DisplayName("원본 메시지의 메타데이터를 유지해야 한다")
        void shouldPreserveMetadata() {
            // given
            RawNewsMessage raw = createRawNewsMessage();

            when(textProcessor.process(anyString())).thenReturn("처리된 내용");
            when(contentValidator.validate(anyString()))
                    .thenReturn(ContentValidator.ValidationResult.success());
            when(nerClient.analyze(anyString())).thenReturn(List.of());

            // when
            Optional<ProcessedNewsMessage> result = service.processNews(raw);

            // then
            assertThat(result).isPresent();
            ProcessedNewsMessage processed = result.get();
            assertThat(processed.publishedAt()).isEqualTo(raw.publishedAt());
            assertThat(processed.publisher()).isEqualTo(raw.publisher());
            assertThat(processed.author()).isEqualTo(raw.author());
            assertThat(processed.category()).isEqualTo(raw.category());
            assertThat(processed.tags()).isEqualTo(raw.tags());
            assertThat(processed.collectedAt()).isEqualTo(raw.collectedAt());
        }
    }

    @Nested
    @DisplayName("processYoutube 메서드")
    class ProcessYoutubeMethod {

        private RawYoutubeMessage createRawYoutubeMessage() {
            return new RawYoutubeMessage(
                    "yt-001",
                    "abc123xyz",
                    "테스트 유튜브 제목",
                    "테스트 유튜브 설명입니다.",
                    "테스트채널",
                    LocalDateTime.now().minusHours(2),
                    "22",
                    List.of("테스트", "유튜브"),
                    1000L,
                    50L,
                    10L,
                    LocalDateTime.now()
            );
        }

        @Test
        @DisplayName("정상적으로 YouTube를 처리해야 한다")
        void shouldProcessYoutubeSuccessfully() {
            // given
            RawYoutubeMessage raw = createRawYoutubeMessage();
            String processedDescription = "테스트 유튜브 설명입니다.";
            List<ExtractedEntity> keywords = List.of(
                    new ExtractedEntity("테스트채널", "ORGANIZATION")
            );

            when(textProcessor.process(raw.description())).thenReturn(processedDescription);
            when(contentValidator.validate(anyString()))
                    .thenReturn(ContentValidator.ValidationResult.success());
            when(nerClient.analyze(anyString())).thenReturn(keywords);

            // when
            Optional<ProcessedYoutubeMessage> result = service.processYoutube(raw);

            // then
            assertThat(result).isPresent();
            ProcessedYoutubeMessage processed = result.get();
            assertThat(processed.id()).isEqualTo(raw.id());
            assertThat(processed.videoId()).isEqualTo(raw.videoId());
            assertThat(processed.title()).isEqualTo(raw.title());
            assertThat(processed.processedDescription()).isEqualTo(processedDescription);
            assertThat(processed.keywords()).containsExactlyInAnyOrderElementsOf(keywords);
        }

        @Test
        @DisplayName("검증 실패 시 빈 Optional을 반환해야 한다")
        void shouldReturnEmptyWhenValidationFails() {
            // given
            RawYoutubeMessage raw = createRawYoutubeMessage();

            when(textProcessor.process(raw.description())).thenReturn("짧음");
            when(contentValidator.validate(anyString()))
                    .thenReturn(ContentValidator.ValidationResult.failure("길이 부족"));

            // when
            Optional<ProcessedYoutubeMessage> result = service.processYoutube(raw);

            // then
            assertThat(result).isEmpty();
            verify(nerClient, never()).analyze(anyString());
        }

        @Test
        @DisplayName("검증 시 제목과 설명을 결합해야 한다")
        void shouldCombineTitleAndDescriptionForValidation() {
            // given
            RawYoutubeMessage raw = createRawYoutubeMessage();
            String processedDescription = "처리된 설명";

            when(textProcessor.process(raw.description())).thenReturn(processedDescription);
            when(contentValidator.validate(anyString()))
                    .thenReturn(ContentValidator.ValidationResult.success());
            when(nerClient.analyze(anyString())).thenReturn(List.of());

            // when
            service.processYoutube(raw);

            // then
            String expectedCombined = raw.title() + " " + processedDescription;
            verify(contentValidator).validate(expectedCombined);
        }

        @Test
        @DisplayName("원본 메시지의 통계 데이터를 유지해야 한다")
        void shouldPreserveStatistics() {
            // given
            RawYoutubeMessage raw = createRawYoutubeMessage();

            when(textProcessor.process(anyString())).thenReturn("처리된 설명");
            when(contentValidator.validate(anyString()))
                    .thenReturn(ContentValidator.ValidationResult.success());
            when(nerClient.analyze(anyString())).thenReturn(List.of());

            // when
            Optional<ProcessedYoutubeMessage> result = service.processYoutube(raw);

            // then
            assertThat(result).isPresent();
            ProcessedYoutubeMessage processed = result.get();
            assertThat(processed.viewCount()).isEqualTo(raw.viewCount());
            assertThat(processed.likeCount()).isEqualTo(raw.likeCount());
            assertThat(processed.commentCount()).isEqualTo(raw.commentCount());
            assertThat(processed.channelTitle()).isEqualTo(raw.channelTitle());
        }
    }
}
