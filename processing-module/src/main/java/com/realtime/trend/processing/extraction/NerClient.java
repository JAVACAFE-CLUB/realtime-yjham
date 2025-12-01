package com.realtime.trend.processing.extraction;

import com.realtime.trend.processing.dto.ExtractedEntity;

import java.util.List;

/**
 * NER(Named Entity Recognition) 클라이언트 인터페이스
 */
public interface NerClient {

    /**
     * 텍스트에서 개체명을 추출합니다.
     *
     * @param text 분석할 텍스트
     * @return 추출된 개체 목록
     */
    List<ExtractedEntity> analyze(String text);

    /**
     * 여러 텍스트에서 개체명을 일괄 추출합니다.
     *
     * @param texts 분석할 텍스트 목록
     * @return 각 텍스트별 추출된 개체 목록
     */
    List<List<ExtractedEntity>> analyzeBatch(List<String> texts);
}
