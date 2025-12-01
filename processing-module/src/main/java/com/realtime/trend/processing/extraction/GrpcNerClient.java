package com.realtime.trend.processing.extraction;

import com.realtime.trend.processing.dto.ExtractedEntity;
import io.grpc.ManagedChannel;
import ner.Ner;
import ner.NERServiceGrpc;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * gRPC 기반 NER 클라이언트 구현
 * 다중 NER 서버에 대한 라운드로빈 로드밸런싱 지원
 */
@Component
public class GrpcNerClient implements NerClient {

    private static final Logger log = LoggerFactory.getLogger(GrpcNerClient.class);

    private final List<NERServiceGrpc.NERServiceBlockingStub> stubs;
    private final AtomicInteger counter = new AtomicInteger(0);

    public GrpcNerClient(List<ManagedChannel> nerChannels) {
        this.stubs = nerChannels.stream()
                .map(NERServiceGrpc::newBlockingStub)
                .toList();
        log.info("GrpcNerClient 초기화: {} 개의 NER 서버에 로드밸런싱", stubs.size());
    }

    /**
     * 라운드로빈 방식으로 다음 Stub 반환
     */
    private NERServiceGrpc.NERServiceBlockingStub getNextStub() {
        int index = Math.abs(counter.getAndIncrement() % stubs.size());
        return stubs.get(index);
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

            NERServiceGrpc.NERServiceBlockingStub stub = getNextStub();
            Ner.NERResponse response = stub.analyze(request);

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

            NERServiceGrpc.NERServiceBlockingStub stub = getNextStub();
            Ner.BatchNERResponse response = stub.analyzeBatch(request);

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
