package com.realtime.trend.serving.controller;

import com.realtime.trend.serving.config.RateLimitConfig;
import com.realtime.trend.serving.dto.KeywordResponse;
import com.realtime.trend.serving.dto.KeywordResponse.KeywordItem;
import com.realtime.trend.serving.dto.KeywordResponse.Metadata;
import com.realtime.trend.serving.service.KeywordService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("KeywordController 단위 테스트")
@WebMvcTest(KeywordController.class)
class KeywordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KeywordService keywordService;

    @MockBean
    private RateLimitConfig rateLimitConfig;

    @Nested
    @DisplayName("GET /api/keywords/today")
    class GetTodayKeywordsTest {

        @Test
        @DisplayName("정상 요청 시 200 OK와 키워드 목록을 반환해야 한다")
        void shouldReturnKeywords() throws Exception {
            // given
            given(rateLimitConfig.tryConsume(anyString())).willReturn(true);

            KeywordResponse response = new KeywordResponse(
                    List.of(
                            new KeywordItem("김연아", "PERSON", 100),
                            new KeywordItem("손흥민", "PERSON", 80)
                    ),
                    new Metadata(2, 10, "all", "all", LocalDateTime.now())
            );
            given(keywordService.getKeywords("all", "all", 10)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/keywords/today"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.keywords").isArray())
                    .andExpect(jsonPath("$.keywords.length()").value(2))
                    .andExpect(jsonPath("$.keywords[0].keyword").value("김연아"))
                    .andExpect(jsonPath("$.keywords[0].type").value("PERSON"))
                    .andExpect(jsonPath("$.keywords[0].count").value(100))
                    .andExpect(jsonPath("$.metadata.totalCount").value(2))
                    .andExpect(jsonPath("$.metadata.source").value("all"));
        }

        @Test
        @DisplayName("파라미터를 지정하면 해당 값으로 조회해야 한다")
        void shouldQueryWithParameters() throws Exception {
            // given
            given(rateLimitConfig.tryConsume(anyString())).willReturn(true);

            KeywordResponse response = new KeywordResponse(
                    List.of(new KeywordItem("서울", "LOCATION", 50)),
                    new Metadata(1, 5, "news", "LOCATION", LocalDateTime.now())
            );
            given(keywordService.getKeywords("news", "LOCATION", 5)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/keywords/today")
                            .param("limit", "5")
                            .param("source", "news")
                            .param("type", "LOCATION"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.keywords[0].keyword").value("서울"))
                    .andExpect(jsonPath("$.metadata.source").value("news"))
                    .andExpect(jsonPath("$.metadata.type").value("LOCATION"));
        }

        @Test
        @DisplayName("Rate limit 초과 시 429 Too Many Requests를 반환해야 한다")
        void whenRateLimitExceeded_shouldReturn429() throws Exception {
            // given
            given(rateLimitConfig.tryConsume(anyString())).willReturn(false);

            // when & then
            mockMvc.perform(get("/api/keywords/today"))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(header().string("Retry-After", "60"))
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("limit이 1 미만이면 400 Bad Request를 반환해야 한다")
        void whenLimitBelowMinimum_shouldReturn400() throws Exception {
            // given
            given(rateLimitConfig.tryConsume(anyString())).willReturn(true);

            // when & then
            mockMvc.perform(get("/api/keywords/today")
                            .param("limit", "0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("limit은 1-100 사이여야 합니다."));
        }

        @Test
        @DisplayName("limit이 100 초과면 400 Bad Request를 반환해야 한다")
        void whenLimitAboveMaximum_shouldReturn400() throws Exception {
            // given
            given(rateLimitConfig.tryConsume(anyString())).willReturn(true);

            // when & then
            mockMvc.perform(get("/api/keywords/today")
                            .param("limit", "101"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("limit은 1-100 사이여야 합니다."));
        }

        @Test
        @DisplayName("잘못된 source 값이면 400 Bad Request를 반환해야 한다")
        void whenInvalidSource_shouldReturn400() throws Exception {
            // given
            given(rateLimitConfig.tryConsume(anyString())).willReturn(true);

            // when & then
            mockMvc.perform(get("/api/keywords/today")
                            .param("source", "invalid"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("source는 all, news, youtube 중 하나여야 합니다."));
        }

        @Test
        @DisplayName("잘못된 type 값이면 400 Bad Request를 반환해야 한다")
        void whenInvalidType_shouldReturn400() throws Exception {
            // given
            given(rateLimitConfig.tryConsume(anyString())).willReturn(true);

            // when & then
            mockMvc.perform(get("/api/keywords/today")
                            .param("type", "INVALID_TYPE"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("type은 all, PERSON, LOCATION, ORGANIZATION 중 하나여야 합니다."));
        }

        @Test
        @DisplayName("X-Forwarded-For 헤더가 있으면 해당 IP로 Rate Limit을 적용해야 한다")
        void whenXForwardedForHeader_shouldUseForwardedIp() throws Exception {
            // given
            given(rateLimitConfig.tryConsume("203.0.113.195")).willReturn(true);

            KeywordResponse response = new KeywordResponse(
                    List.of(),
                    new Metadata(0, 10, "all", "all", LocalDateTime.now())
            );
            given(keywordService.getKeywords(anyString(), anyString(), anyInt())).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/keywords/today")
                            .header("X-Forwarded-For", "203.0.113.195, 70.41.3.18, 150.172.238.178"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("source가 'youtube'일 때 정상 처리되어야 한다")
        void whenSourceIsYoutube_shouldProcess() throws Exception {
            // given
            given(rateLimitConfig.tryConsume(anyString())).willReturn(true);

            KeywordResponse response = new KeywordResponse(
                    List.of(new KeywordItem("유튜버", "PERSON", 30)),
                    new Metadata(1, 10, "youtube", "all", LocalDateTime.now())
            );
            given(keywordService.getKeywords("youtube", "all", 10)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/keywords/today")
                            .param("source", "youtube"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.metadata.source").value("youtube"));
        }

        @Test
        @DisplayName("type이 'ORGANIZATION'일 때 정상 처리되어야 한다")
        void whenTypeIsOrganization_shouldProcess() throws Exception {
            // given
            given(rateLimitConfig.tryConsume(anyString())).willReturn(true);

            KeywordResponse response = new KeywordResponse(
                    List.of(new KeywordItem("삼성전자", "ORGANIZATION", 200)),
                    new Metadata(1, 10, "all", "ORGANIZATION", LocalDateTime.now())
            );
            given(keywordService.getKeywords("all", "ORGANIZATION", 10)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/keywords/today")
                            .param("type", "ORGANIZATION"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.metadata.type").value("ORGANIZATION"));
        }
    }
}
