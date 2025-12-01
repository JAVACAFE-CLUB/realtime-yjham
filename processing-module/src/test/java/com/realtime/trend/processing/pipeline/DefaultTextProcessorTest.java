package com.realtime.trend.processing.pipeline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DefaultTextProcessor 단위 테스트")
class DefaultTextProcessorTest {

    private DefaultTextProcessor textProcessor;

    @BeforeEach
    void setUp() {
        textProcessor = new DefaultTextProcessor();
    }

    @Nested
    @DisplayName("process 메서드")
    class ProcessMethod {

        @Test
        @DisplayName("HTML 태그를 제거해야 한다")
        void shouldRemoveHtmlTags() {
            // given
            String html = "<p>안녕하세요 <strong>테스트</strong>입니다.</p>";

            // when
            String result = textProcessor.process(html);

            // then
            assertThat(result).isEqualTo("안녕하세요 테스트입니다.");
        }

        @Test
        @DisplayName("복잡한 HTML 문서를 처리해야 한다")
        void shouldHandleComplexHtml() {
            // given
            String html = """
                <!DOCTYPE html>
                <html>
                <head><title>테스트</title></head>
                <body>
                    <div class="content">
                        <h1>제목입니다</h1>
                        <p>본문 내용입니다.</p>
                    </div>
                </body>
                </html>
                """;

            // when
            String result = textProcessor.process(html);

            // then
            assertThat(result).contains("제목입니다");
            assertThat(result).contains("본문 내용입니다");
            assertThat(result).doesNotContain("<");
            assertThat(result).doesNotContain(">");
        }

        @Test
        @DisplayName("URL을 제거해야 한다")
        void shouldRemoveUrls() {
            // given
            String text = "자세한 내용은 https://example.com/news/123 에서 확인하세요.";

            // when
            String result = textProcessor.process(text);

            // then
            assertThat(result).doesNotContain("https://");
            assertThat(result).doesNotContain("example.com");
        }

        @Test
        @DisplayName("HTTP URL도 제거해야 한다")
        void shouldRemoveHttpUrls() {
            // given
            String text = "링크: http://test.co.kr/path?query=value";

            // when
            String result = textProcessor.process(text);

            // then
            assertThat(result).doesNotContain("http://");
            assertThat(result).doesNotContain("test.co.kr");
        }

        @Test
        @DisplayName("이메일 주소를 제거해야 한다")
        void shouldRemoveEmails() {
            // given
            String text = "문의는 contact@example.com 으로 보내주세요.";

            // when
            String result = textProcessor.process(text);

            // then
            assertThat(result).doesNotContain("contact@example.com");
            assertThat(result).doesNotContain("@");
        }

        @Test
        @DisplayName("제어 문자를 제거해야 한다")
        void shouldRemoveSpecialChars() {
            // given
            String text = "테스트\u0000문자\u001F입니다";

            // when
            String result = textProcessor.process(text);

            // then
            assertThat(result).isEqualTo("테스트문자입니다");
        }

        @Test
        @DisplayName("연속 공백을 단일 공백으로 정규화해야 한다")
        void shouldNormalizeWhitespace() {
            // given
            String text = "여러   공백이    있는   텍스트";

            // when
            String result = textProcessor.process(text);

            // then
            assertThat(result).isEqualTo("여러 공백이 있는 텍스트");
        }

        @Test
        @DisplayName("줄바꿈을 공백으로 정규화해야 한다")
        void shouldNormalizeNewlines() {
            // given
            String text = "첫째줄\n둘째줄\r\n셋째줄";

            // when
            String result = textProcessor.process(text);

            // then
            assertThat(result).isEqualTo("첫째줄 둘째줄 셋째줄");
        }

        @Test
        @DisplayName("null 입력 시 빈 문자열을 반환해야 한다")
        void shouldReturnEmptyForNull() {
            // when
            String result = textProcessor.process(null);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("빈 문자열 입력 시 빈 문자열을 반환해야 한다")
        void shouldReturnEmptyForBlank() {
            // when
            String result = textProcessor.process("   ");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("모든 전처리를 순서대로 적용해야 한다")
        void shouldApplyAllProcessingInOrder() {
            // given
            String text = """
                <p>한국은행이 기준금리를 동결했습니다.</p>
                자세한 내용: https://news.example.com/article/123
                문의: reporter@news.com
                """;

            // when
            String result = textProcessor.process(text);

            // then
            assertThat(result)
                    .contains("한국은행이 기준금리를 동결했습니다")
                    .doesNotContain("<p>")
                    .doesNotContain("https://")
                    .doesNotContain("@");
        }

        @Test
        @DisplayName("한글 텍스트를 올바르게 처리해야 한다")
        void shouldHandleKoreanText() {
            // given
            String text = "대한민국 서울특별시에서 개최된 행사입니다.";

            // when
            String result = textProcessor.process(text);

            // then
            assertThat(result).isEqualTo("대한민국 서울특별시에서 개최된 행사입니다.");
        }
    }
}
