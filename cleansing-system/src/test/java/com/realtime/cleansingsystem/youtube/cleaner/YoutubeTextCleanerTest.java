package com.realtime.cleansingsystem.youtube.cleaner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("YoutubeTextCleaner 테스트")
class YoutubeTextCleanerTest {

    private YoutubeTextCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner = new YoutubeTextCleaner();
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
    @DisplayName("URL 제거 - http")
    void removeHttpUrl() {
        // given
        String text = "동영상 설명입니다. http://example.com 방문해주세요.";

        // when
        String result = cleaner.clean(text);

        // then
        assertThat(result).isEqualTo("동영상 설명입니다. 방문해주세요.");
        assertThat(result).doesNotContain("http://");
    }

    @Test
    @DisplayName("URL 제거 - https")
    void removeHttpsUrl() {
        // given
        String text = "LE SSERAFIM 'SPAGHETTI'\nhttps://le-sserafim.lnk.to/SPAGHETTI\n스트리밍하세요!";

        // when
        String result = cleaner.clean(text);

        // then
        assertThat(result).contains("LE SSERAFIM 'SPAGHETTI'");
        assertThat(result).contains("스트리밍하세요!");
        assertThat(result).doesNotContain("https://");
    }

    @Test
    @DisplayName("여러 URL 제거")
    void removeMultipleUrls() {
        // given
        String text = "링크1: https://example.com\n링크2: http://test.com\n내용";

        // when
        String result = cleaner.clean(text);

        // then
        assertThat(result).contains("링크1:");
        assertThat(result).contains("링크2:");
        assertThat(result).contains("내용");
        assertThat(result).doesNotContain("http");
        assertThat(result).doesNotContain("example.com");
    }

    @Test
    @DisplayName("이모지 제거")
    void removeEmojis() {
        // given
        String text = "LE SSERAFIM 💿 'SPAGHETTI' 🎵 스트리밍";

        // when
        String result = cleaner.clean(text);

        // then
        assertThat(result).contains("LE SSERAFIM");
        assertThat(result).contains("'SPAGHETTI'");
        assertThat(result).contains("스트리밍");
        assertThat(result).doesNotContain("💿");
        assertThat(result).doesNotContain("🎵");
    }

    @Test
    @DisplayName("공백 정규화")
    void normalizeSpaces() {
        // given
        String text = "LE SSERAFIM    'SPAGHETTI'   뮤직비디오";

        // when
        String result = cleaner.clean(text);

        // then
        assertThat(result).isEqualTo("LE SSERAFIM 'SPAGHETTI' 뮤직비디오");
        assertThat(result).doesNotContain("  ");
    }

    @Test
    @DisplayName("줄바꿈 정규화")
    void normalizeNewlines() {
        // given
        String text = "첫 번째 줄\n\n\n\n두 번째 줄";

        // when
        String result = cleaner.clean(text);

        // then
        assertThat(result).isEqualTo("첫 번째 줄\n\n두 번째 줄");
    }

    @Test
    @DisplayName("앞뒤 공백 제거")
    void trimText() {
        // given
        String text = "   LE SSERAFIM 'SPAGHETTI'   ";

        // when
        String result = cleaner.clean(text);

        // then
        assertThat(result).isEqualTo("LE SSERAFIM 'SPAGHETTI'");
        assertThat(result).doesNotStartWith(" ");
        assertThat(result).doesNotEndWith(" ");
    }

    @Test
    @DisplayName("실제 유튜브 설명 정제")
    void cleanRealYoutubeDescription() {
        // given
        String text = "LE SSERAFIM (르세라핌) 'SPAGHETTI (feat. j-hope of BTS)' OFFICIAL MV\n\n" +
                "💿 https://le-sserafim.lnk.to/SPAGHETTI\n\n\n" +
                "🎵 Spotify: https://open.spotify.com/track/xxx\n" +
                "🎵 Apple Music: https://music.apple.com/xxx\n\n" +
                "지금 바로  스트리밍하세요!";

        // when
        String result = cleaner.clean(text);

        // then
        assertThat(result).contains("LE SSERAFIM (르세라핌) 'SPAGHETTI");
        assertThat(result).contains("지금 바로 스트리밍하세요!");
        assertThat(result).doesNotContain("http");
        assertThat(result).doesNotContain("https");
        assertThat(result).doesNotContain("💿");
        assertThat(result).doesNotContain("🎵");
        assertThat(result).doesNotContain("  ");  // 연속된 공백 없음
    }

    @Test
    @DisplayName("복합적인 정제 작업")
    void complexCleaning() {
        // given
        String text = "  새로운   뮤직비디오 💿\n\n\n\nhttps://example.com\n에서\t\t확인하세요!  ";

        // when
        String result = cleaner.clean(text);

        // then
        assertThat(result).contains("새로운 뮤직비디오");
        assertThat(result).contains("에서 확인하세요!");
        assertThat(result).doesNotContain("http");
        assertThat(result).doesNotContain("💿");
        assertThat(result).doesNotContain("  ");
        assertThat(result).doesNotContain("\t\t");
    }
}
