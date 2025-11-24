package com.realtime.trend.serving.controller;

import com.realtime.trend.serving.config.RateLimitConfig;
import com.realtime.trend.serving.dto.KeywordResponse;
import com.realtime.trend.serving.dto.KeywordResponse.KeywordItem;
import com.realtime.trend.serving.dto.KeywordResponse.Metadata;
import com.realtime.trend.serving.service.KeywordService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(KeywordController.class)
class KeywordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KeywordService keywordService;

    @MockBean
    private RateLimitConfig rateLimitConfig;

    @Test
    @DisplayName("오늘의 키워드를 조회한다")
    void getTodayKeywords_returnsKeywords() throws Exception {
        // given
        when(rateLimitConfig.tryConsume(anyString())).thenReturn(true);

        KeywordResponse response = new KeywordResponse(
                List.of(
                        new KeywordItem("삼성전자", "ORG", 100),
                        new KeywordItem("이재용", "PER", 50)
                ),
                new Metadata(2, 10, "all", "all", LocalDateTime.now())
        );
        when(keywordService.getKeywords("all", "all", 10)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/keywords/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keywords").isArray())
                .andExpect(jsonPath("$.keywords.length()").value(2))
                .andExpect(jsonPath("$.keywords[0].keyword").value("삼성전자"))
                .andExpect(jsonPath("$.keywords[0].count").value(100))
                .andExpect(jsonPath("$.metadata.totalCount").value(2));
    }

    @Test
    @DisplayName("source 파라미터로 필터링한다")
    void getTodayKeywords_withSource_filtersResults() throws Exception {
        // given
        when(rateLimitConfig.tryConsume(anyString())).thenReturn(true);

        KeywordResponse response = new KeywordResponse(
                List.of(new KeywordItem("BTS", "ORG", 200)),
                new Metadata(1, 10, "youtube", "all", LocalDateTime.now())
        );
        when(keywordService.getKeywords("youtube", "all", 10)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/keywords/today")
                        .param("source", "youtube"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.source").value("youtube"));
    }

    @Test
    @DisplayName("type 파라미터로 필터링한다")
    void getTodayKeywords_withType_filtersResults() throws Exception {
        // given
        when(rateLimitConfig.tryConsume(anyString())).thenReturn(true);

        KeywordResponse response = new KeywordResponse(
                List.of(new KeywordItem("테스트", "PERSON", 30)),
                new Metadata(1, 10, "all", "PERSON", LocalDateTime.now())
        );
        when(keywordService.getKeywords("all", "PERSON", 10)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/keywords/today")
                        .param("type", "PERSON"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.type").value("PERSON"));
    }

    @Test
    @DisplayName("limit이 0 이하면 400 에러를 반환한다")
    void getTodayKeywords_withInvalidLimit_returnsBadRequest() throws Exception {
        // given
        when(rateLimitConfig.tryConsume(anyString())).thenReturn(true);

        // when & then
        mockMvc.perform(get("/api/keywords/today")
                        .param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("limit은 1-100 사이여야 합니다."));
    }

    @Test
    @DisplayName("limit이 100 초과면 400 에러를 반환한다")
    void getTodayKeywords_withLimitOver100_returnsBadRequest() throws Exception {
        // given
        when(rateLimitConfig.tryConsume(anyString())).thenReturn(true);

        // when & then
        mockMvc.perform(get("/api/keywords/today")
                        .param("limit", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("limit은 1-100 사이여야 합니다."));
    }

    @Test
    @DisplayName("유효하지 않은 source는 400 에러를 반환한다")
    void getTodayKeywords_withInvalidSource_returnsBadRequest() throws Exception {
        // given
        when(rateLimitConfig.tryConsume(anyString())).thenReturn(true);

        // when & then
        mockMvc.perform(get("/api/keywords/today")
                        .param("source", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("유효하지 않은 type은 400 에러를 반환한다")
    void getTodayKeywords_withInvalidType_returnsBadRequest() throws Exception {
        // given
        when(rateLimitConfig.tryConsume(anyString())).thenReturn(true);

        // when & then
        mockMvc.perform(get("/api/keywords/today")
                        .param("type", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Rate limit 초과시 429 에러를 반환한다")
    void getTodayKeywords_whenRateLimitExceeded_returnsTooManyRequests() throws Exception {
        // given
        when(rateLimitConfig.tryConsume(anyString())).thenReturn(false);

        // when & then
        mockMvc.perform(get("/api/keywords/today"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.message").value("요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요."));
    }

    @Test
    @DisplayName("custom limit으로 조회한다")
    void getTodayKeywords_withCustomLimit_usesLimit() throws Exception {
        // given
        when(rateLimitConfig.tryConsume(anyString())).thenReturn(true);

        KeywordResponse response = new KeywordResponse(
                List.of(),
                new Metadata(0, 50, "all", "all", LocalDateTime.now())
        );
        when(keywordService.getKeywords("all", "all", 50)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/keywords/today")
                        .param("limit", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.limit").value(50));
    }
}
