package com.realtime.trend.processing.pipeline;

import com.realtime.trend.processing.config.ProcessingProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 품질 기반 콘텐츠 검증 구현
 */
@Component
public class QualityContentValidator implements ContentValidator {

    private final int minContentLength;

    private static final Set<String> SPAM_KEYWORDS = Set.of(
            "광고", "홍보", "무료", "할인", "이벤트", "쿠폰",
            "클릭", "바로가기", "문의", "상담"
    );

    public QualityContentValidator(ProcessingProperties properties) {
        this.minContentLength = properties.minContentLength();
    }

    @Override
    public ValidationResult validate(String content) {
        if (content == null || content.isBlank()) {
            return ValidationResult.failure("본문이 비어있습니다");
        }

        if (content.length() < minContentLength) {
            return ValidationResult.failure(
                    String.format("본문 길이 부족: %d자 (최소 %d자)", content.length(), minContentLength));
        }

        if (containsSpamKeywords(content)) {
            return ValidationResult.failure("스팸 키워드 포함");
        }

        return ValidationResult.success();
    }

    private boolean containsSpamKeywords(String content) {
        String lowerContent = content.toLowerCase();
        return SPAM_KEYWORDS.stream()
                .anyMatch(lowerContent::contains);
    }
}
