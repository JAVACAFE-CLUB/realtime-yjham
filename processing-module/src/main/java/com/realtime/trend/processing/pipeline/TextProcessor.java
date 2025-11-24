package com.realtime.trend.processing.pipeline;

/**
 * 텍스트 전처리 인터페이스
 */
public interface TextProcessor {

    /**
     * 텍스트를 전처리합니다.
     * HTML 태그, URL, 이메일, 특수문자 제거 및 공백 정규화 등을 수행합니다.
     *
     * @param text 원본 텍스트
     * @return 전처리된 텍스트
     */
    String process(String text);
}
