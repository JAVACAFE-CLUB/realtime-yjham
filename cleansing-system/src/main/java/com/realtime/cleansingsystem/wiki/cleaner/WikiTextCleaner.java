package com.realtime.cleansingsystem.wiki.cleaner;

import org.springframework.stereotype.Component;

/**
 * 위키피디아 텍스트 정제기
 * 1. 위키 마크업 제거
 *    - 템플릿: {{템플릿명|...}} → 제거
 *    - 내부 링크: [[링크|표시명]] → "표시명" 추출
 *    - 볼드: '''텍스트''' → "텍스트"
 *    - 이탤릭: ''텍스트'' → "텍스트"
 *    - 표: {| ... |} → 제거
 *    - 참조: <ref>...</ref> → 제거
 * 2. 공백 정규화
 * 3. 줄바꿈을 공백으로 변환 (실시간 검색어 키워드 추출에 최적화)
 * 4. 특수문자 정리
 * 5. 앞뒤 공백 제거
 */
@Component
public class WikiTextCleaner {

    /**
     * 위키피디아 텍스트 정제
     */
    public String clean(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String cleaned = text;

        // 1. 위키 마크업 제거
        cleaned = removeWikiMarkup(cleaned);

        // 2. 제어 문자 제거
        cleaned = removeControlCharacters(cleaned);

        // 3. 공백 정규화
        cleaned = normalizeSpaces(cleaned);

        // 4. 줄바꿈 정규화
        cleaned = normalizeNewlines(cleaned);

        // 5. 앞뒤 공백 제거
        cleaned = cleaned.trim();

        return cleaned;
    }

    /**
     * 위키 마크업 제거
     */
    private String removeWikiMarkup(String text) {
        String cleaned = text;

        // 템플릿 제거: {{...}}
        cleaned = removeTemplates(cleaned);

        // 표 제거: {|...|}
        cleaned = removeTables(cleaned);

        // 참조 제거: <ref>...</ref>
        cleaned = cleaned.replaceAll("<ref[^>]*>.*?</ref>", "");
        cleaned = cleaned.replaceAll("<ref[^/]*/>", "");

        // HTML 태그 제거
        cleaned = cleaned.replaceAll("<[^>]+>", "");

        // 파일/이미지 링크 제거: [[파일:...]], [[File:...]]
        cleaned = cleaned.replaceAll("\\[\\[(파일|File|그림|Image):[^\\]]+\\]\\]", "");

        // 내부 링크 처리: [[링크|표시명]] → 표시명
        cleaned = cleaned.replaceAll("\\[\\[([^|\\]]+)\\|([^\\]]+)\\]\\]", "$2");
        // 내부 링크 처리: [[링크]] → 링크
        cleaned = cleaned.replaceAll("\\[\\[([^\\]]+)\\]\\]", "$1");

        // 외부 링크 제거: [http://...]
        cleaned = cleaned.replaceAll("\\[https?://[^\\s\\]]+\\s*([^\\]]*)\\]", "$1");

        // 볼드 제거: '''텍스트''' → 텍스트
        cleaned = cleaned.replaceAll("'''([^']+)'''", "$1");

        // 이탤릭 제거: ''텍스트'' → 텍스트
        cleaned = cleaned.replaceAll("''([^']+)''", "$1");

        // 헤딩 제거: ==텍스트== → 텍스트
        cleaned = cleaned.replaceAll("={2,}([^=]+)={2,}", "$1");

        // 리스트 마커 제거 (줄바꿈 + 마커)
        cleaned = cleaned.replaceAll("\\n[*#:\\-]+\\s*", " ");

        return cleaned;
    }

    /**
     * 템플릿 제거 (중첩된 경우 고려)
     */
    private String removeTemplates(String text) {
        // 중첩된 {{...}} 처리를 위한 반복
        String cleaned = text;
        int maxIterations = 10;  // 무한 루프 방지
        for (int i = 0; i < maxIterations; i++) {
            String before = cleaned;
            // 가장 안쪽 템플릿부터 제거
            cleaned = cleaned.replaceAll("\\{\\{[^{}]*\\}\\}", "");
            if (before.equals(cleaned)) {
                break;  // 더 이상 변화가 없으면 종료
            }
        }
        return cleaned;
    }

    /**
     * 표 제거 (중첩된 경우 고려)
     */
    private String removeTables(String text) {
        // 중첩된 {|...|} 처리를 위한 반복
        String cleaned = text;
        int maxIterations = 10;  // 무한 루프 방지
        for (int i = 0; i < maxIterations; i++) {
            String before = cleaned;
            // 가장 안쪽 표부터 제거
            cleaned = cleaned.replaceAll("\\{\\|[^{}]*\\|\\}", "");
            if (before.equals(cleaned)) {
                break;
        }
        }
        return cleaned;
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
