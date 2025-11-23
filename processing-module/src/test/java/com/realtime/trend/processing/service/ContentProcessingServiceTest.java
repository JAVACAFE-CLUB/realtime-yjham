package com.realtime.trend.processing.service;

import com.realtime.trend.processing.dto.*;
import com.realtime.trend.processing.grpc.NerGrpcClient;
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

@DisplayName("ContentProcessingService 테스트")
@ExtendWith(MockitoExtension.class)
class ContentProcessingServiceTest {

    @Mock
    private TextPreprocessor textPreprocessor;

    @Mock
    private QualityValidator qualityValidator;

    @Mock
    private NerGrpcClient nerGrpcClient;

    private ContentProcessingService contentProcessingService;

    @BeforeEach
    void setUp() {
        contentProcessingService = new ContentProcessingService(
                textPreprocessor,
                qualityValidator,
                nerGrpcClient
        );
    }

    @Nested
    @DisplayName("뉴스 처리")
    class ProcessNews {

        @Test
        @DisplayName("유효한 뉴스 처리 성공")
        void shouldProcessValidNews() {
            // given
            RawNewsMessage rawNews = createRawNewsMessage();
            String processedContent = "전처리된 본문 내용입니다.";
            List<ExtractedEntity> keywords = List.of(
                    new ExtractedEntity("삼성전자", "ORGANIZATION"),
                    new ExtractedEntity("서울", "LOCATION")
            );

            when(textPreprocessor.preprocess(anyString())).thenReturn(processedContent);
            when(qualityValidator.validate(anyString()))
                    .thenReturn(new QualityValidator.ValidationResult(true, null));
            when(nerGrpcClient.analyze(anyString())).thenReturn(keywords);

            // when
            Optional<ProcessedNewsMessage> result = contentProcessingService.processNews(rawNews);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(rawNews.id());
            assertThat(result.get().keywords()).hasSize(2);
            assertThat(result.get().content()).isEqualTo(processedContent);

            verify(textPreprocessor).preprocess(rawNews.content());
            verify(qualityValidator).validate(processedContent);
            verify(nerGrpcClient).analyze(anyString());
        }

        @Test
        @DisplayName("품질 검증 실패 시 빈 결과 반환")
        void shouldReturnEmptyWhenValidationFails() {
            // given
            RawNewsMessage rawNews = createRawNewsMessage();
            String processedContent = "짧은 내용";

            when(textPreprocessor.preprocess(anyString())).thenReturn(processedContent);
            when(qualityValidator.validate(anyString()))
                    .thenReturn(new QualityValidator.ValidationResult(false, "본문 길이 부족"));

            // when
            Optional<ProcessedNewsMessage> result = contentProcessingService.processNews(rawNews);

            // then
            assertThat(result).isEmpty();
            verify(nerGrpcClient, never()).analyze(anyString());
        }

        @Test
        @DisplayName("NER 결과가 빈 리스트여도 처리 성공")
        void shouldProcessNewsWithEmptyKeywords() {
            // given
            RawNewsMessage rawNews = createRawNewsMessage();
            String processedContent = "전처리된 본문 내용입니다.";

            when(textPreprocessor.preprocess(anyString())).thenReturn(processedContent);
            when(qualityValidator.validate(anyString()))
                    .thenReturn(new QualityValidator.ValidationResult(true, null));
            when(nerGrpcClient.analyze(anyString())).thenReturn(List.of());

            // when
            Optional<ProcessedNewsMessage> result = contentProcessingService.processNews(rawNews);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().keywords()).isEmpty();
        }

        private RawNewsMessage createRawNewsMessage() {
            return new RawNewsMessage(
                    "news-001",
                    "https://example.com/news/1",
                    "삼성전자 신제품 발표",
                    "<p>삼성전자가 서울에서 신제품을 발표했다.</p>",
                    LocalDateTime.now(),
                    "테스트신문",
                    "홍길동",
                    "IT",
                    List.of("삼성", "IT"),
                    LocalDateTime.now()
            );
        }
    }

    @Nested
    @DisplayName("유튜브 처리")
    class ProcessYoutube {

        @Test
        @DisplayName("유효한 유튜브 처리 성공")
        void shouldProcessValidYoutube() {
            // given
            RawYoutubeMessage rawYoutube = createRawYoutubeMessage();
            String processedDescription = "전처리된 설명입니다.";
            List<ExtractedEntity> keywords = List.of(
                    new ExtractedEntity("손흥민", "PERSON"),
                    new ExtractedEntity("토트넘", "ORGANIZATION")
            );

            when(textPreprocessor.preprocess(anyString())).thenReturn(processedDescription);
            when(qualityValidator.validate(anyString()))
                    .thenReturn(new QualityValidator.ValidationResult(true, null));
            when(nerGrpcClient.analyze(anyString())).thenReturn(keywords);

            // when
            Optional<ProcessedYoutubeMessage> result = contentProcessingService.processYoutube(rawYoutube);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().videoId()).isEqualTo(rawYoutube.videoId());
            assertThat(result.get().keywords()).hasSize(2);

            verify(textPreprocessor).preprocess(rawYoutube.description());
            verify(qualityValidator).validate(anyString());
            verify(nerGrpcClient).analyze(anyString());
        }

        @Test
        @DisplayName("품질 검증 실패 시 빈 결과 반환")
        void shouldReturnEmptyWhenValidationFails() {
            // given
            RawYoutubeMessage rawYoutube = createRawYoutubeMessage();

            when(textPreprocessor.preprocess(anyString())).thenReturn("짧음");
            when(qualityValidator.validate(anyString()))
                    .thenReturn(new QualityValidator.ValidationResult(false, "스팸 키워드 포함"));

            // when
            Optional<ProcessedYoutubeMessage> result = contentProcessingService.processYoutube(rawYoutube);

            // then
            assertThat(result).isEmpty();
            verify(nerGrpcClient, never()).analyze(anyString());
        }

        private RawYoutubeMessage createRawYoutubeMessage() {
            return new RawYoutubeMessage(
                    "yt-001",
                    "dQw4w9WgXcQ",
                    "손흥민 골 모음",
                    "토트넘 손흥민 선수의 멋진 골 모음입니다.",
                    "스포츠채널",
                    LocalDateTime.now(),
                    "17",
                    List.of("축구", "손흥민"),
                    1000000L,
                    50000L,
                    5000L,
                    LocalDateTime.now()
            );
        }
    }
}
