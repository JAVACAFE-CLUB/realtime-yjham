package com.realtime.cleansingsystem.wiki.cleaner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WikiTextCleaner 테스트")
class WikiTextCleanerTest {

    private WikiTextCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner = new WikiTextCleaner();
    }

    @Test
    @DisplayName("null 입력 시 빈 문자열 반환")
    void cleanNullText() {
        // when
        String result = cleaner.clean(null);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("템플릿 제거")
    void removeTemplates() {
        // given
        String text = "{{선거 정보|선거명=대한민국 제16대}}텍스트 내용";

        // when
        String result = cleaner.clean(text);

        // then
        assertThat(result).isEqualTo("텍스트 내용");
        assertThat(result).doesNotContain("{{");
        assertThat(result).doesNotContain("}}");
    }

    @Test
    @DisplayName("중첩된 템플릿 제거")
    void removeNestedTemplates() {
        // given
        String text = "{{외부|내용={{내부|값}}}}텍스트";

        // when
        String result = cleaner.clean(text);

        // then
        assertThat(result).isEqualTo("텍스트");
    }

    @Test
    @DisplayName("내부 링크 처리 - 표시명 추출")
    void extractInternalLinkLabel() {
        // given
        String text = "[[대한민국의 대통령|대통령]]을 선출";

        // when
        String result = cleaner.clean(text);

        // then
        assertThat(result).isEqualTo("대통령을 선출");
        assertThat(result).doesNotContain("[[");
        assertThat(result).doesNotContain("]]");
    }

    @Test
    @DisplayName("내부 링크 처리 - 링크만 있는 경우")
    void extractInternalLinkOnly() {
        // given
        String text = "[[대한민국]]의 역사";

        // when
        String result = cleaner.clean(text);

        // then
        assertThat(result).isEqualTo("대한민국의 역사");
    }

    @Test
    @DisplayName("볼드 제거")
    void removeBold() {
        // given
        String text = "'''대한민국 제16대 대통령 선거'''는 중요한 선거였다.";

        // when
        String result = cleaner.clean(text);

        // then
        assertThat(result).isEqualTo("대한민국 제16대 대통령 선거는 중요한 선거였다.");
        assertThat(result).doesNotContain("'''");
    }

    @Test
    @DisplayName("이탤릭 제거")
    void removeItalic() {
        // given
        String text = "''이탤릭 텍스트''입니다.";

        // when
        String result = cleaner.clean(text);

        // then
        assertThat(result).isEqualTo("이탤릭 텍스트입니다.");
        assertThat(result).doesNotContain("''");
    }

    @Test
    @DisplayName("표 제거")
    void removeTables() {
        // given
        String text = "텍스트{|표 내용\n|셀1|셀2\n|}계속";

        // when
        String result = cleaner.clean(text);

        // then
        assertThat(result).isEqualTo("텍스트계속");
        assertThat(result).doesNotContain("{|");
        assertThat(result).doesNotContain("|}");
    }

    @Test
    @DisplayName("참조 제거")
    void removeReferences() {
        // given
        String text = "텍스트<ref>참조 내용</ref>계속";

        // when
        String result = cleaner.clean(text);

        // then
        assertThat(result).isEqualTo("텍스트계속");
        assertThat(result).doesNotContain("<ref>");
        assertThat(result).doesNotContain("</ref>");
    }

    @Test
    @DisplayName("HTML 태그 제거")
    void removeHtmlTags() {
        // given
        String text = "<div>텍스트</div><br>내용";

        // when
        String result = cleaner.clean(text);

        // then
        assertThat(result).isEqualTo("텍스트내용");
        assertThat(result).doesNotContain("<");
        assertThat(result).doesNotContain(">");
    }

    @Test
    @DisplayName("파일/이미지 링크 제거")
    void removeFileLinks() {
        // given
        String text = "[[파일:Example.jpg|섬네일]]텍스트[[File:Image.png]]내용";

        // when
        String result = cleaner.clean(text);

        // then
        assertThat(result).isEqualTo("텍스트내용");
        assertThat(result).doesNotContain("파일:");
        assertThat(result).doesNotContain("File:");
    }

    @Test
    @DisplayName("헤딩 제거")
    void removeHeadings() {
        // given
        String text = "==제목==\n내용\n===소제목===\n내용2";

        // when
        String result = cleaner.clean(text);

        // then
        assertThat(result).contains("제목");
        assertThat(result).contains("소제목");
        assertThat(result).doesNotContain("==");
        assertThat(result).doesNotContain("===");
    }

    @Test
    @DisplayName("실제 위키 텍스트 정제")
    void cleanRealWikiText() {
        // given
        String text = "{{선거 정보\n| 선거명 = 대한민국 제16대}}\n" +
                "'''대한민국 제16대 대통령 선거'''는 [[대한민국의 대통령|대통령]]을 선출하는 선거였다.<ref>참조</ref>\n\n\n\n" +
                "==개요==\n" +
                "이 선거는 ''중요한'' 선거였다.";

        // when
        String result = cleaner.clean(text);

        // then
        assertThat(result).contains("대한민국 제16대 대통령 선거는");
        assertThat(result).contains("대통령을 선출하는 선거였다");
        assertThat(result).contains("개요");
        assertThat(result).contains("이 선거는 중요한 선거였다");
        assertThat(result).doesNotContain("{{");
        assertThat(result).doesNotContain("'''");
        assertThat(result).doesNotContain("[[");
        assertThat(result).doesNotContain("<ref>");
        assertThat(result).doesNotContain("==");
    }

    @Test
    @DisplayName("공백과 줄바꿈 정규화")
    void normalizeSpacesAndNewlines() {
        // given
        String text = "텍스트   내용\n\n\n\n다음  단락";

        // when
        String result = cleaner.clean(text);

        // then
        assertThat(result).isEqualTo("텍스트 내용\n\n다음 단락");
    }
}
