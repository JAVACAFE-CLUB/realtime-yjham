package com.realtime.cleansingsystem.news.cleaner;

import org.springframework.stereotype.Component;

/**
 * 뉴스 텍스트 정제기
 * 1. HTML 태그 제거 (이미 수집 시스템에서 처리됨)
 * 2. 공백 정규화 (연속된 공백 → 단일 공백)
 * 3. 줄바꿈을 공백으로 변환 (실시간 검색어 키워드 추출에 최적화)
 * 4. 특수문자 정리 (제어 문자, 불필요한 유니코드 제거)
 * 5. 앞뒤 공백 제거 (trim)
 */
@Component
public class NewsTextCleaner {

    /**
     * 뉴스 텍스트 정제
     */
    public String clean(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String cleaned = text;

        // 1. 제어 문자 제거 (탭, 캐리지 리턴 등은 유지)
        cleaned = removeControlCharacters(cleaned);

        // 2. 공백 정규화
        cleaned = normalizeSpaces(cleaned);

        // 3. 줄바꿈 정규화
        cleaned = normalizeNewlines(cleaned);

        // 4. 앞뒤 공백 제거
        cleaned = cleaned.trim();

        return cleaned;
    }

    /**
     * 제어 문자 제거 (탭, 줄바꿈, 캐리지 리턴 제외)
     */
    private String removeControlCharacters(String text) {
        // \p{Cntrl}은 제어 문자, 하지만 \t, \n, \r은 유지
        return text.replaceAll("[\\p{Cntrl}&&[^\t\n\r]]", "");
    }

    /**
     * 공백 정규화 (연속된 공백 → 단일 공백)
     */
    private String normalizeSpaces(String text) {
        // 연속된 공백 문자를 단일 공백으로 변환
        return text.replaceAll("[ \t]+", " ");
    }

    /**
     * 줄바꿈 정규화 (연속된 줄바꿈 → 최대 2개)
     */
    private String normalizeNewlines(String text) {
        // 모든 줄바꿈을 공백으로 변환
        return text.replaceAll("\r?\n", " ");
    }
}
