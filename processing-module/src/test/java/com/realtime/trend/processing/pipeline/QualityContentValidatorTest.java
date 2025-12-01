package com.realtime.trend.processing.pipeline;

import com.realtime.trend.processing.config.ProcessingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QualityContentValidator 단위 테스트")
class QualityContentValidatorTest {

    private QualityContentValidator validator;
    private static final int MIN_CONTENT_LENGTH = 50;

    @BeforeEach
    void setUp() {
        ProcessingProperties properties = new ProcessingProperties(
                MIN_CONTENT_LENGTH,
                new ProcessingProperties.Retry(3, 1000, 2.0)
        );
        validator = new QualityContentValidator(properties);
    }

    @Nested
    @DisplayName("validate 메서드")
    class ValidateMethod {

        @Test
        @DisplayName("유효한 콘텐츠는 통과해야 한다")
        void shouldPassForValidContent() {
            // given
            String content = "한국은행이 기준금리를 동결하기로 결정했습니다. 이번 결정은 경제 상황을 고려한 것으로 분석됩니다.";

            // when
            ContentValidator.ValidationResult result = validator.validate(content);

            // then
            assertThat(result.valid()).isTrue();
            assertThat(result.reason()).isNull();
        }

        @Test
        @DisplayName("null 입력은 실패해야 한다")
        void shouldFailForNull() {
            // when
            ContentValidator.ValidationResult result = validator.validate(null);

            // then
            assertThat(result.valid()).isFalse();
            assertThat(result.reason()).contains("비어있습니다");
        }

        @Test
        @DisplayName("빈 문자열은 실패해야 한다")
        void shouldFailForEmpty() {
            // when
            ContentValidator.ValidationResult result = validator.validate("");

            // then
            assertThat(result.valid()).isFalse();
            assertThat(result.reason()).contains("비어있습니다");
        }

        @Test
        @DisplayName("공백만 있는 문자열은 실패해야 한다")
        void shouldFailForBlank() {
            // when
            ContentValidator.ValidationResult result = validator.validate("   ");

            // then
            assertThat(result.valid()).isFalse();
            assertThat(result.reason()).contains("비어있습니다");
        }

        @Test
        @DisplayName("최소 길이 미달은 실패해야 한다")
        void shouldFailForTooShort() {
            // given
            String shortContent = "짧은 콘텐츠";

            // when
            ContentValidator.ValidationResult result = validator.validate(shortContent);

            // then
            assertThat(result.valid()).isFalse();
            assertThat(result.reason()).contains("길이 부족");
        }

        @Test
        @DisplayName("정확히 최소 길이인 콘텐츠는 통과해야 한다")
        void shouldPassForExactMinLength() {
            // given
            String content = "가".repeat(MIN_CONTENT_LENGTH);

            // when
            ContentValidator.ValidationResult result = validator.validate(content);

            // then
            assertThat(result.valid()).isTrue();
        }

        @Test
        @DisplayName("최소 길이보다 1자 부족하면 실패해야 한다")
        void shouldFailForOneLessThanMinLength() {
            // given
            String content = "가".repeat(MIN_CONTENT_LENGTH - 1);

            // when
            ContentValidator.ValidationResult result = validator.validate(content);

            // then
            assertThat(result.valid()).isFalse();
        }

        @Nested
        @DisplayName("스팸 키워드 필터링")
        class SpamKeywordFiltering {

            private String createValidContentWith(String keyword) {
                // 최소 50자 이상의 콘텐츠 생성
                return "이것은 충분히 긴 콘텐츠입니다. " + keyword + " 관련 내용이 포함되어 있습니다. 품질 검증을 위한 추가 텍스트입니다.";
            }

            @Test
            @DisplayName("'광고' 키워드 포함 시 실패해야 한다")
            void shouldFailForAdKeyword() {
                // given
                String content = createValidContentWith("광고");

                // when
                ContentValidator.ValidationResult result = validator.validate(content);

                // then
                assertThat(result.valid()).isFalse();
                assertThat(result.reason()).contains("스팸");
            }

            @Test
            @DisplayName("'홍보' 키워드 포함 시 실패해야 한다")
            void shouldFailForPromotionKeyword() {
                // given
                String content = createValidContentWith("홍보");

                // when
                ContentValidator.ValidationResult result = validator.validate(content);

                // then
                assertThat(result.valid()).isFalse();
            }

            @Test
            @DisplayName("'무료' 키워드 포함 시 실패해야 한다")
            void shouldFailForFreeKeyword() {
                // given
                String content = createValidContentWith("무료");

                // when
                ContentValidator.ValidationResult result = validator.validate(content);

                // then
                assertThat(result.valid()).isFalse();
            }

            @Test
            @DisplayName("'할인' 키워드 포함 시 실패해야 한다")
            void shouldFailForDiscountKeyword() {
                // given
                String content = createValidContentWith("할인");

                // when
                ContentValidator.ValidationResult result = validator.validate(content);

                // then
                assertThat(result.valid()).isFalse();
            }

            @Test
            @DisplayName("'이벤트' 키워드 포함 시 실패해야 한다")
            void shouldFailForEventKeyword() {
                // given
                String content = createValidContentWith("이벤트");

                // when
                ContentValidator.ValidationResult result = validator.validate(content);

                // then
                assertThat(result.valid()).isFalse();
            }

            @Test
            @DisplayName("'쿠폰' 키워드 포함 시 실패해야 한다")
            void shouldFailForCouponKeyword() {
                // given
                String content = createValidContentWith("쿠폰");

                // when
                ContentValidator.ValidationResult result = validator.validate(content);

                // then
                assertThat(result.valid()).isFalse();
            }

            @Test
            @DisplayName("'클릭' 키워드 포함 시 실패해야 한다")
            void shouldFailForClickKeyword() {
                // given
                String content = createValidContentWith("클릭");

                // when
                ContentValidator.ValidationResult result = validator.validate(content);

                // then
                assertThat(result.valid()).isFalse();
            }

            @Test
            @DisplayName("'바로가기' 키워드 포함 시 실패해야 한다")
            void shouldFailForLinkKeyword() {
                // given
                String content = createValidContentWith("바로가기");

                // when
                ContentValidator.ValidationResult result = validator.validate(content);

                // then
                assertThat(result.valid()).isFalse();
            }

            @Test
            @DisplayName("스팸 키워드가 없는 콘텐츠는 통과해야 한다")
            void shouldPassForContentWithoutSpamKeywords() {
                // given
                String content = "한국은행이 기준금리를 동결하기로 결정했습니다. 경제 전문가들은 이번 결정을 긍정적으로 평가했습니다.";

                // when
                ContentValidator.ValidationResult result = validator.validate(content);

                // then
                assertThat(result.valid()).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("ValidationResult 레코드")
    class ValidationResultRecord {

        @Test
        @DisplayName("success()는 valid=true를 반환해야 한다")
        void successShouldReturnValid() {
            // when
            ContentValidator.ValidationResult result = ContentValidator.ValidationResult.success();

            // then
            assertThat(result.valid()).isTrue();
            assertThat(result.reason()).isNull();
        }

        @Test
        @DisplayName("failure()는 valid=false와 사유를 반환해야 한다")
        void failureShouldReturnInvalidWithReason() {
            // given
            String reason = "테스트 실패 사유";

            // when
            ContentValidator.ValidationResult result = ContentValidator.ValidationResult.failure(reason);

            // then
            assertThat(result.valid()).isFalse();
            assertThat(result.reason()).isEqualTo(reason);
        }
    }
}
