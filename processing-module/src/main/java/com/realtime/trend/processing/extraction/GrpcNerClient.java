package com.realtime.trend.processing.extraction;

import com.realtime.trend.processing.dto.ExtractedEntity;
import ner.Ner;
import ner.NERServiceGrpc;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * gRPC 기반 NER 클라이언트 구현
 */
@Component
public class GrpcNerClient implements NerClient {

    private static final Logger log = LoggerFactory.getLogger(GrpcNerClient.class);

    private final NERServiceGrpc.NERServiceBlockingStub nerServiceStub;

    public GrpcNerClient(NERServiceGrpc.NERServiceBlockingStub nerServiceStub) {
        this.nerServiceStub = nerServiceStub;
    }

    @Override
    public List<ExtractedEntity> analyze(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        try {
            Ner.TextRequest request = Ner.TextRequest.newBuilder()
                    .setText(text)
                    .build();

            Ner.NERResponse response = nerServiceStub.analyze(request);

            return response.getEntitiesList().stream()
                    .map(entity -> new ExtractedEntity(
                            entity.getKeyword(),
                            entity.getType()
                    ))
                    .collect(Collectors.toList());

        } catch (StatusRuntimeException e) {
            log.error("NER 서비스 호출 실패: {}", e.getStatus(), e);
            throw new NerClientException("NER 서비스 호출 실패", e);
        }
    }

    @Override
    public List<List<ExtractedEntity>> analyzeBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            Ner.BatchTextRequest request = Ner.BatchTextRequest.newBuilder()
                    .addAllTexts(texts)
                    .build();

            Ner.BatchNERResponse response = nerServiceStub.analyzeBatch(request);

            return response.getResponsesList().stream()
                    .map(nerResponse -> nerResponse.getEntitiesList().stream()
                            .map(entity -> new ExtractedEntity(
                                    entity.getKeyword(),
                                    entity.getType()
                            ))
                            .collect(Collectors.toList()))
                    .collect(Collectors.toList());

        } catch (StatusRuntimeException e) {
            log.error("NER 배치 서비스 호출 실패: {}", e.getStatus(), e);
            throw new NerClientException("NER 배치 서비스 호출 실패", e);
        }
    }
}
