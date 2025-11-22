package com.realtime.trend.processing.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 데이터 품질 검증 서비스
 */
@Service
public class QualityValidator {

    private final int minContentLength;

    private static final Set<String> SPAM_KEYWORDS = Set.of(
            "광고", "홍보", "무료", "할인", "이벤트", "쿠폰",
            "클릭", "바로가기", "문의", "상담"
    );

    public QualityValidator(@Value("${processing.min-content-length}") int minContentLength) {
        this.minContentLength = minContentLength;
    }

    public boolean isValid(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }

        // 최소 길이 검증
        if (content.length() < minContentLength) {
            return false;
        }

        // 스팸 키워드 검증
        if (containsSpamKeywords(content)) {
            return false;
        }

        return true;
    }

    private boolean containsSpamKeywords(String content) {
        String lowerContent = content.toLowerCase();
        return SPAM_KEYWORDS.stream()
                .anyMatch(lowerContent::contains);
    }

    public ValidationResult validate(String content) {
        if (content == null || content.isBlank()) {
            return new ValidationResult(false, "본문이 비어있습니다");
        }

        if (content.length() < minContentLength) {
            return new ValidationResult(false,
                    String.format("본문 길이 부족: %d자 (최소 %d자)", content.length(), minContentLength));
        }

        if (containsSpamKeywords(content)) {
            return new ValidationResult(false, "스팸 키워드 포함");
        }

        return new ValidationResult(true, null);
    }

    public record ValidationResult(boolean valid, String reason) {
    }
}
