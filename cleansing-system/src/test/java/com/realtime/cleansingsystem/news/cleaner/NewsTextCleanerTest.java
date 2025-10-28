package com.realtime.cleansingsystem.news.cleaner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NewsTextCleaner 테스트")
class NewsTextCleanerTest {

    private NewsTextCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner = new NewsTextCleaner();
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
    @DisplayName("빈 문자열 입력 시 빈 문자열 반환")
    void cleanEmptyText() {
        // when
        String result = cleaner.clean("");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("공백만 있는 문자열 입력 시 빈 문자열 반환")
    void cleanBlankText() {
        // when
        String result = cleaner.clean("   ");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("연속된 공백을 단일 공백으로 정규화")
    void normalizeSpaces() {
        // given
        String text = "현대건설이    국내   기업   중  최초로";

        // when
        String result = cleaner.clean(text);

        // then
        assertThat(result).isEqualTo("현대건설이 국내 기업 중 최초로");
    }

    @Test
    @DisplayName("연속된 줄바꿈을 최대 2개로 정규화")
    void normalizeNewlines() {
        // given
        String text = "첫 번째 문단입니다.\n\n\n\n두 번째 문단입니다.";

        // when
        String result = cleaner.clean(text);

        // then
        assertThat(result).isEqualTo("첫 번째 문단입니다.\n\n두 번째 문단입니다.");
    }

    @Test
    @DisplayName("제어 문자 제거 (탭, 줄바꿈 제외)")
    void removeControlCharacters() {
        // given
        String text = "정상 텍스트\u0000제어문자\u0001포함";

        // when
        String result = cleaner.clean(text);

        // then
        assertThat(result).isEqualTo("정상 텍스트제어문자포함");
    }

    @Test
    @DisplayName("앞뒤 공백 제거")
    void trimText() {
        // given
        String text = "   앞뒤 공백이 있는 텍스트   ";

        // when
        String result = cleaner.clean(text);

        // then
        assertThat(result).isEqualTo("앞뒤 공백이 있는 텍스트");
    }

    @Test
    @DisplayName("복합적인 정제 작업")
    void complexCleaning() {
        // given
        String text = "  현대건설이   국내 기업 중\n\n\n\n최초로  미국 원전 설계를\t\t수주했습니다.  ";

        // when
        String result = cleaner.clean(text);

        // then
        assertThat(result).isEqualTo("현대건설이 국내 기업 중\n\n최초로 미국 원전 설계를 수주했습니다.");
    }

    @Test
    @DisplayName("실제 뉴스 텍스트 정제")
    void cleanRealNewsText() {
        // given
        String text = "현대건설이  국내  기업  중  최초로\n\n\n미국 원전  설계를 수주했습니다.\n\n\n\n" +
                "이번 계약은  총  규모가\t\t1조원이 넘는  대규모  프로젝트입니다.  ";

        // when
        String result = cleaner.clean(text);

        // then
        assertThat(result).contains("현대건설이 국내 기업 중 최초로");
        assertThat(result).contains("미국 원전 설계를 수주했습니다.");
        assertThat(result).contains("이번 계약은 총 규모가 1조원이 넘는 대규모 프로젝트입니다.");
        assertThat(result).doesNotContain("  ");  // 연속된 공백 없음
        assertThat(result).doesNotContain("\t\t");  // 연속된 탭 없음
        assertThat(result).doesNotStartWith(" ");  // 앞 공백 없음
        assertThat(result).doesNotEndWith(" ");  // 뒤 공백 없음
    }
}
