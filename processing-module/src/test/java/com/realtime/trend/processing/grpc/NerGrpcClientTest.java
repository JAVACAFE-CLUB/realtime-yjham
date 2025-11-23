package com.realtime.trend.processing.grpc;

import com.realtime.trend.processing.dto.ExtractedEntity;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import ner.NERServiceGrpc;
import ner.Ner;
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

@DisplayName("NerGrpcClient 테스트")
@ExtendWith(MockitoExtension.class)
class NerGrpcClientTest {

    @Mock
    private NERServiceGrpc.NERServiceBlockingStub nerServiceStub;

    private NerGrpcClient nerGrpcClient;

    @BeforeEach
    void setUp() {
        nerGrpcClient = new NerGrpcClient(nerServiceStub);
    }

    @Nested
    @DisplayName("analyze 메서드")
    class AnalyzeMethod {

        @Test
        @DisplayName("null 입력 시 빈 리스트 반환")
        void shouldReturnEmptyListForNull() {
            List<ExtractedEntity> result = nerGrpcClient.analyze(null);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("빈 문자열 입력 시 빈 리스트 반환")
        void shouldReturnEmptyListForEmptyString() {
            List<ExtractedEntity> result = nerGrpcClient.analyze("");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("공백만 있는 문자열 입력 시 빈 리스트 반환")
        void shouldReturnEmptyListForBlankString() {
            List<ExtractedEntity> result = nerGrpcClient.analyze("   ");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("유효한 텍스트 분석 성공")
        void shouldAnalyzeValidText() {
            // given
            Ner.NERResponse response = Ner.NERResponse.newBuilder()
                    .addEntities(Ner.Entity.newBuilder()
                            .setKeyword("삼성전자")
                            .setType("ORGANIZATION")
                            .build())
                    .addEntities(Ner.Entity.newBuilder()
                            .setKeyword("서울")
                            .setType("LOCATION")
                            .build())
                    .build();

            when(nerServiceStub.analyze(any(Ner.TextRequest.class))).thenReturn(response);

            // when
            List<ExtractedEntity> result = nerGrpcClient.analyze("삼성전자가 서울에서 발표했다.");

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(ExtractedEntity::keyword)
                    .containsExactly("삼성전자", "서울");
            assertThat(result).extracting(ExtractedEntity::type)
                    .containsExactly("ORGANIZATION", "LOCATION");
        }

        @Test
        @DisplayName("gRPC 오류 시 RuntimeException 발생")
        void shouldThrowExceptionOnGrpcError() {
            // given
            when(nerServiceStub.analyze(any(Ner.TextRequest.class)))
                    .thenThrow(new StatusRuntimeException(Status.UNAVAILABLE));

            // when & then
            assertThatThrownBy(() -> nerGrpcClient.analyze("테스트"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("NER 서비스 호출 실패");
        }
    }

    @Nested
    @DisplayName("analyzeBatch 메서드")
    class AnalyzeBatchMethod {

        @Test
        @DisplayName("null 입력 시 빈 리스트 반환")
        void shouldReturnEmptyListForNull() {
            List<List<ExtractedEntity>> result = nerGrpcClient.analyzeBatch(null);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("빈 리스트 입력 시 빈 리스트 반환")
        void shouldReturnEmptyListForEmptyList() {
            List<List<ExtractedEntity>> result = nerGrpcClient.analyzeBatch(List.of());
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("배치 분석 성공")
        void shouldAnalyzeBatch() {
            // given
            Ner.BatchNERResponse response = Ner.BatchNERResponse.newBuilder()
                    .addResponses(Ner.NERResponse.newBuilder()
                            .addEntities(Ner.Entity.newBuilder()
                                    .setKeyword("삼성전자")
                                    .setType("ORGANIZATION")
                                    .build())
                            .build())
                    .addResponses(Ner.NERResponse.newBuilder()
                            .addEntities(Ner.Entity.newBuilder()
                                    .setKeyword("손흥민")
                                    .setType("PERSON")
                                    .build())
                            .build())
                    .build();

            when(nerServiceStub.analyzeBatch(any(Ner.BatchTextRequest.class))).thenReturn(response);

            // when
            List<List<ExtractedEntity>> result = nerGrpcClient.analyzeBatch(
                    List.of("삼성전자 뉴스", "손흥민 소식")
            );

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0)).hasSize(1);
            assertThat(result.get(0).get(0).keyword()).isEqualTo("삼성전자");
            assertThat(result.get(1).get(0).keyword()).isEqualTo("손흥민");
        }

        @Test
        @DisplayName("gRPC 오류 시 RuntimeException 발생")
        void shouldThrowExceptionOnGrpcError() {
            // given
            when(nerServiceStub.analyzeBatch(any(Ner.BatchTextRequest.class)))
                    .thenThrow(new StatusRuntimeException(Status.INTERNAL));

            // when & then
            assertThatThrownBy(() -> nerGrpcClient.analyzeBatch(List.of("테스트")))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("NER 배치 서비스 호출 실패");
        }
    }
}
