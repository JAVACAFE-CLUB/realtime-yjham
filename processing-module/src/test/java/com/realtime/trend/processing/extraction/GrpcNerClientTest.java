package com.realtime.trend.processing.extraction;

import com.realtime.trend.processing.dto.ExtractedEntity;
import ner.Ner;
import ner.NERServiceGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GrpcNerClient 단위 테스트")
class GrpcNerClientTest {

    @Mock
    private NERServiceGrpc.NERServiceBlockingStub nerServiceStub;

    private GrpcNerClient grpcNerClient;

    @BeforeEach
    void setUp() {
        grpcNerClient = new GrpcNerClient(nerServiceStub);
    }

    @Nested
    @DisplayName("analyze 메서드")
    class AnalyzeMethod {

        @Test
        @DisplayName("정상 응답 시 엔티티 목록을 반환해야 한다")
        void shouldReturnEntities() {
            // given
            String text = "한국은행이 기준금리를 동결했습니다.";
            Ner.NERResponse response = Ner.NERResponse.newBuilder()
                    .addEntities(Ner.Entity.newBuilder()
                            .setKeyword("한국은행")
                            .setType("ORGANIZATION")
                            .build())
                    .addEntities(Ner.Entity.newBuilder()
                            .setKeyword("금리")
                            .setType("TERM")
                            .build())
                    .build();

            when(nerServiceStub.analyze(any(Ner.TextRequest.class))).thenReturn(response);

            // when
            List<ExtractedEntity> result = grpcNerClient.analyze(text);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(ExtractedEntity::keyword)
                    .containsExactly("한국은행", "금리");
            assertThat(result).extracting(ExtractedEntity::type)
                    .containsExactly("ORGANIZATION", "TERM");
        }

        @Test
        @DisplayName("빈 응답 시 빈 리스트를 반환해야 한다")
        void shouldReturnEmptyListForEmptyResponse() {
            // given
            String text = "엔티티가 없는 텍스트";
            Ner.NERResponse response = Ner.NERResponse.newBuilder().build();

            when(nerServiceStub.analyze(any(Ner.TextRequest.class))).thenReturn(response);

            // when
            List<ExtractedEntity> result = grpcNerClient.analyze(text);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("null 입력 시 빈 리스트를 반환해야 한다")
        void shouldReturnEmptyForNull() {
            // when
            List<ExtractedEntity> result = grpcNerClient.analyze(null);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("빈 문자열 입력 시 빈 리스트를 반환해야 한다")
        void shouldReturnEmptyForBlank() {
            // when
            List<ExtractedEntity> result = grpcNerClient.analyze("   ");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("gRPC 오류 시 NerClientException을 던져야 한다")
        void shouldThrowOnGrpcError() {
            // given
            String text = "테스트 텍스트";
            when(nerServiceStub.analyze(any(Ner.TextRequest.class)))
                    .thenThrow(new StatusRuntimeException(Status.UNAVAILABLE));

            // when & then
            assertThatThrownBy(() -> grpcNerClient.analyze(text))
                    .isInstanceOf(NerClientException.class)
                    .hasMessageContaining("NER 서비스 호출 실패");
        }

        @Test
        @DisplayName("gRPC DEADLINE_EXCEEDED 오류 처리")
        void shouldHandleDeadlineExceeded() {
            // given
            String text = "테스트 텍스트";
            when(nerServiceStub.analyze(any(Ner.TextRequest.class)))
                    .thenThrow(new StatusRuntimeException(Status.DEADLINE_EXCEEDED));

            // when & then
            assertThatThrownBy(() -> grpcNerClient.analyze(text))
                    .isInstanceOf(NerClientException.class);
        }
    }

    @Nested
    @DisplayName("analyzeBatch 메서드")
    class AnalyzeBatchMethod {

        @Test
        @DisplayName("배치 요청 시 각 텍스트별 결과를 반환해야 한다")
        void shouldReturnBatchResults() {
            // given
            List<String> texts = List.of("첫 번째 텍스트", "두 번째 텍스트");

            Ner.BatchNERResponse response = Ner.BatchNERResponse.newBuilder()
                    .addResponses(Ner.NERResponse.newBuilder()
                            .addEntities(Ner.Entity.newBuilder()
                                    .setKeyword("엔티티1")
                                    .setType("TYPE1")
                                    .build())
                            .build())
                    .addResponses(Ner.NERResponse.newBuilder()
                            .addEntities(Ner.Entity.newBuilder()
                                    .setKeyword("엔티티2")
                                    .setType("TYPE2")
                                    .build())
                            .build())
                    .build();

            when(nerServiceStub.analyzeBatch(any(Ner.BatchTextRequest.class))).thenReturn(response);

            // when
            List<List<ExtractedEntity>> result = grpcNerClient.analyzeBatch(texts);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0)).hasSize(1);
            assertThat(result.get(0).get(0).keyword()).isEqualTo("엔티티1");
            assertThat(result.get(1)).hasSize(1);
            assertThat(result.get(1).get(0).keyword()).isEqualTo("엔티티2");
        }

        @Test
        @DisplayName("null 입력 시 빈 리스트를 반환해야 한다")
        void shouldReturnEmptyForNull() {
            // when
            List<List<ExtractedEntity>> result = grpcNerClient.analyzeBatch(null);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("빈 리스트 입력 시 빈 리스트를 반환해야 한다")
        void shouldReturnEmptyForEmptyList() {
            // when
            List<List<ExtractedEntity>> result = grpcNerClient.analyzeBatch(List.of());

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("gRPC 오류 시 NerClientException을 던져야 한다")
        void shouldThrowOnGrpcError() {
            // given
            List<String> texts = List.of("테스트");
            when(nerServiceStub.analyzeBatch(any(Ner.BatchTextRequest.class)))
                    .thenThrow(new StatusRuntimeException(Status.INTERNAL));

            // when & then
            assertThatThrownBy(() -> grpcNerClient.analyzeBatch(texts))
                    .isInstanceOf(NerClientException.class)
                    .hasMessageContaining("배치 서비스 호출 실패");
        }
    }
}
