package com.realtime.trend.processing.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TextPreprocessor 테스트")
class TextPreprocessorTest {

    private TextPreprocessor textPreprocessor;

    @BeforeEach
    void setUp() {
        textPreprocessor = new TextPreprocessor();
    }

    @Nested
    @DisplayName("null/빈 문자열 처리")
    class NullAndEmptyHandling {

        @Test
        @DisplayName("null 입력 시 빈 문자열 반환")
        void shouldReturnEmptyForNull() {
            String result = textPreprocessor.preprocess(null);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("빈 문자열 입력 시 빈 문자열 반환")
        void shouldReturnEmptyForEmptyString() {
            String result = textPreprocessor.preprocess("");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("공백만 있는 문자열 입력 시 빈 문자열 반환")
        void shouldReturnEmptyForBlankString() {
            String result = textPreprocessor.preprocess("   ");
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("HTML 태그 제거")
    class HtmlTagRemoval {

        @Test
        @DisplayName("기본 HTML 태그 제거")
        void shouldRemoveBasicHtmlTags() {
            String input = "<p>안녕하세요</p>";
            String result = textPreprocessor.preprocess(input);
            assertThat(result).isEqualTo("안녕하세요");
        }

        @Test
        @DisplayName("중첩된 HTML 태그 제거")
        void shouldRemoveNestedHtmlTags() {
            String input = "<div><p><strong>중요한</strong> 내용</p></div>";
            String result = textPreprocessor.preprocess(input);
            assertThat(result).isEqualTo("중요한 내용");
        }

        @Test
        @DisplayName("HTML 엔티티 변환")
        void shouldConvertHtmlEntities() {
            String input = "&lt;script&gt;alert('xss')&lt;/script&gt;";
            String result = textPreprocessor.preprocess(input);
            assertThat(result).doesNotContain("<script>");
        }
    }

    @Nested
    @DisplayName("URL 제거")
    class UrlRemoval {

        @Test
        @DisplayName("HTTP URL 제거")
        void shouldRemoveHttpUrls() {
            String input = "자세한 내용은 http://example.com 을 참조하세요.";
            String result = textPreprocessor.preprocess(input);
            assertThat(result).doesNotContain("http://");
            assertThat(result).contains("자세한 내용은");
        }

        @Test
        @DisplayName("HTTPS URL 제거")
        void shouldRemoveHttpsUrls() {
            String input = "방문하기: https://www.example.com/path?query=value";
            String result = textPreprocessor.preprocess(input);
            assertThat(result).doesNotContain("https://");
        }

        @Test
        @DisplayName("복잡한 URL 제거")
        void shouldRemoveComplexUrls() {
            String input = "링크: https://subdomain.example.co.kr/path/to/page?a=1&b=2#section";
            String result = textPreprocessor.preprocess(input);
            assertThat(result).doesNotContain("example");
        }
    }

    @Nested
    @DisplayName("이메일 제거")
    class EmailRemoval {

        @Test
        @DisplayName("기본 이메일 제거")
        void shouldRemoveBasicEmails() {
            String input = "문의: contact@example.com 으로 연락주세요.";
            String result = textPreprocessor.preprocess(input);
            assertThat(result).doesNotContain("@");
            assertThat(result).contains("문의:");
        }

        @Test
        @DisplayName("복잡한 이메일 제거")
        void shouldRemoveComplexEmails() {
            String input = "담당자: john.doe+tag@sub.example.co.kr 입니다.";
            String result = textPreprocessor.preprocess(input);
            assertThat(result).doesNotContain("@");
        }
    }

    @Nested
    @DisplayName("특수 문자 제거")
    class SpecialCharRemoval {

        @Test
        @DisplayName("제어 문자 제거")
        void shouldRemoveControlCharacters() {
            String input = "텍스트\u0000중간에\u001F제어문자";
            String result = textPreprocessor.preprocess(input);
            assertThat(result).doesNotContain("\u0000");
            assertThat(result).doesNotContain("\u001F");
        }
    }

    @Nested
    @DisplayName("공백 정규화")
    class WhitespaceNormalization {

        @Test
        @DisplayName("연속 공백을 단일 공백으로 변환")
        void shouldNormalizeMultipleSpaces() {
            String input = "여러   개의    공백이   있습니다";
            String result = textPreprocessor.preprocess(input);
            assertThat(result).isEqualTo("여러 개의 공백이 있습니다");
        }

        @Test
        @DisplayName("줄바꿈을 공백으로 변환")
        void shouldConvertNewlinesToSpaces() {
            String input = "첫째 줄\n둘째 줄\r\n셋째 줄";
            String result = textPreprocessor.preprocess(input);
            assertThat(result).doesNotContain("\n");
            assertThat(result).doesNotContain("\r");
        }

        @Test
        @DisplayName("앞뒤 공백 제거")
        void shouldTrimWhitespace() {
            String input = "  앞뒤 공백  ";
            String result = textPreprocessor.preprocess(input);
            assertThat(result).isEqualTo("앞뒤 공백");
        }
    }

    @Nested
    @DisplayName("복합 전처리")
    class CombinedPreprocessing {

        @Test
        @DisplayName("모든 전처리 규칙 적용")
        void shouldApplyAllRules() {
            String input = """
                <div>
                    <p>삼성전자가 서울에서 신제품을 발표했다.</p>
                    <p>자세한 내용: https://example.com</p>
                    <p>문의: press@samsung.com</p>
                </div>
                """;

            String result = textPreprocessor.preprocess(input);

            assertThat(result)
                    .contains("삼성전자")
                    .contains("서울")
                    .doesNotContain("<")
                    .doesNotContain(">")
                    .doesNotContain("https://")
                    .doesNotContain("@");
        }
    }
}
