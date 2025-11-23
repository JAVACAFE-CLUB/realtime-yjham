package com.realtime.trend.processing.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QualityValidator 테스트")
class QualityValidatorTest {

    private QualityValidator qualityValidator;
    private static final int MIN_CONTENT_LENGTH = 50;

    @BeforeEach
    void setUp() {
        qualityValidator = new QualityValidator(MIN_CONTENT_LENGTH);
    }

    @Nested
    @DisplayName("isValid 메서드")
    class IsValidMethod {

        @Test
        @DisplayName("null 입력 시 false 반환")
        void shouldReturnFalseForNull() {
            boolean result = qualityValidator.isValid(null);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("빈 문자열 입력 시 false 반환")
        void shouldReturnFalseForEmptyString() {
            boolean result = qualityValidator.isValid("");
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("공백만 있는 문자열 입력 시 false 반환")
        void shouldReturnFalseForBlankString() {
            boolean result = qualityValidator.isValid("   ");
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("최소 길이 미만 시 false 반환")
        void shouldReturnFalseForShortContent() {
            String shortContent = "짧은 내용";
            assertThat(shortContent.length()).isLessThan(MIN_CONTENT_LENGTH);

            boolean result = qualityValidator.isValid(shortContent);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("스팸 키워드 포함 시 false 반환")
        void shouldReturnFalseForSpamContent() {
            String spamContent = "이것은 충분히 긴 내용입니다. ".repeat(5) + "무료 이벤트에 참여하세요!";
            assertThat(spamContent.length()).isGreaterThanOrEqualTo(MIN_CONTENT_LENGTH);

            boolean result = qualityValidator.isValid(spamContent);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("유효한 콘텐츠 시 true 반환")
        void shouldReturnTrueForValidContent() {
            String validContent = "삼성전자가 서울에서 신제품을 발표했다. 이재용 회장이 참석하여 새로운 기술에 대해 설명했다.";
            assertThat(validContent.length()).isGreaterThanOrEqualTo(MIN_CONTENT_LENGTH);

            boolean result = qualityValidator.isValid(validContent);
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("validate 메서드")
    class ValidateMethod {

        @Test
        @DisplayName("null 입력 시 실패 결과 반환")
        void shouldReturnInvalidForNull() {
            var result = qualityValidator.validate(null);

            assertThat(result.valid()).isFalse();
            assertThat(result.reason()).contains("비어있습니다");
        }

        @Test
        @DisplayName("최소 길이 미만 시 길이 정보 포함 실패 결과 반환")
        void shouldReturnInvalidWithLengthInfo() {
            String shortContent = "짧은 내용";

            var result = qualityValidator.validate(shortContent);

            assertThat(result.valid()).isFalse();
            assertThat(result.reason()).contains("길이 부족");
            assertThat(result.reason()).contains(String.valueOf(shortContent.length()));
            assertThat(result.reason()).contains(String.valueOf(MIN_CONTENT_LENGTH));
        }

        @Test
        @DisplayName("스팸 키워드 포함 시 스팸 사유 반환")
        void shouldReturnInvalidWithSpamReason() {
            String spamContent = "이것은 충분히 긴 내용입니다. ".repeat(5) + "광고 문의는 여기로!";

            var result = qualityValidator.validate(spamContent);

            assertThat(result.valid()).isFalse();
            assertThat(result.reason()).contains("스팸");
        }

        @Test
        @DisplayName("유효한 콘텐츠 시 성공 결과 반환")
        void shouldReturnValidForGoodContent() {
            String validContent = "삼성전자가 서울에서 신제품을 발표했다. 이재용 회장이 참석하여 새로운 기술에 대해 설명했다.";

            var result = qualityValidator.validate(validContent);

            assertThat(result.valid()).isTrue();
            assertThat(result.reason()).isNull();
        }
    }

    @Nested
    @DisplayName("스팸 키워드 검증")
    class SpamKeywordValidation {

        @Test
        @DisplayName("광고 키워드 감지")
        void shouldDetectAdKeyword() {
            String content = "이것은 충분히 긴 내용입니다. ".repeat(5) + "광고입니다.";
            assertThat(qualityValidator.isValid(content)).isFalse();
        }

        @Test
        @DisplayName("홍보 키워드 감지")
        void shouldDetectPromoKeyword() {
            String content = "이것은 충분히 긴 내용입니다. ".repeat(5) + "홍보 목적입니다.";
            assertThat(qualityValidator.isValid(content)).isFalse();
        }

        @Test
        @DisplayName("이벤트 키워드 감지")
        void shouldDetectEventKeyword() {
            String content = "이것은 충분히 긴 내용입니다. ".repeat(5) + "특별 이벤트 진행중!";
            assertThat(qualityValidator.isValid(content)).isFalse();
        }

        @Test
        @DisplayName("대소문자 구분 없이 스팸 감지")
        void shouldDetectSpamCaseInsensitive() {
            String content = "이것은 충분히 긴 내용입니다. ".repeat(5) + "할인 SALE!";
            assertThat(qualityValidator.isValid(content)).isFalse();
        }
    }
}
