package com.realtime.trend.processing.pipeline;

/**
 * 콘텐츠 품질 검증 인터페이스
 */
public interface ContentValidator {

    /**
     * 콘텐츠 품질을 검증합니다.
     *
     * @param content 검증할 콘텐츠
     * @return 검증 결과
     */
    ValidationResult validate(String content);

    /**
     * 검증 결과
     *
     * @param valid  검증 통과 여부
     * @param reason 실패 시 사유
     */
    record ValidationResult(boolean valid, String reason) {
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult failure(String reason) {
            return new ValidationResult(false, reason);
        }
    }
}
