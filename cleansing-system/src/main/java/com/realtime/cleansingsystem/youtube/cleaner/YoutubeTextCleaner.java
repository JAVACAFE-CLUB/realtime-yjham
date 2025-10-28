package com.realtime.cleansingsystem.youtube.cleaner;

import org.springframework.stereotype.Component;

/**
 * 유튜브 텍스트 정제기
 * 1. URL 제거 (description 내의 모든 URL)
 * 2. 이모지 제거
 * 3. 공백 정규화
 * 4. 줄바꿈을 공백으로 변환 (실시간 검색어 키워드 추출에 최적화)
 * 5. 특수문자 정리
 * 6. 앞뒤 공백 제거
 */
@Component
public class YoutubeTextCleaner {

    /**
     * 유튜브 설명 텍스트 정제
     */
    public String clean(String description) {
        if (description == null || description.isBlank()) {
            return "";
        }

        String cleaned = description;

        // 1. URL 제거
        cleaned = removeUrls(cleaned);

        // 2. 이모지 제거
        cleaned = removeEmojis(cleaned);

        // 3. 제어 문자 제거
        cleaned = removeControlCharacters(cleaned);

        // 4. 공백 정규화
        cleaned = normalizeSpaces(cleaned);

        // 5. 줄바꿈 정규화
        cleaned = normalizeNewlines(cleaned);

        // 6. 앞뒤 공백 제거
        cleaned = cleaned.trim();

        return cleaned;
    }

    /**
     * URL 제거 (http, https)
     */
    private String removeUrls(String text) {
        // http:// 또는 https://로 시작하는 URL 제거
        return text.replaceAll("https?://\\S+", "");
    }

    /**
     * 이모지 제거
     * 유니코드 이모지 범위: U+1F600-U+1F64F, U+1F300-U+1F5FF, U+1F680-U+1F6FF, etc.
     */
    private String removeEmojis(String text) {
        // 이모지 유니코드 범위 제거
        return text.replaceAll("[\\p{So}\\p{Sk}]", "");
    }

    /**
     * 제어 문자 제거 (탭, 줄바꿈, 캐리지 리턴 제외)
     */
    private String removeControlCharacters(String text) {
        return text.replaceAll("[\\p{Cntrl}&&[^\t\n\r]]", "");
    }

    /**
     * 공백 정규화 (연속된 공백 → 단일 공백)
     */
    private String normalizeSpaces(String text) {
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
