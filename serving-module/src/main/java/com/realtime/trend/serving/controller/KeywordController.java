package com.realtime.trend.serving.controller;

import com.realtime.trend.serving.config.RateLimitConfig;
import com.realtime.trend.serving.dto.KeywordResponse;
import com.realtime.trend.serving.service.KeywordService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/keywords")
public class KeywordController {

    private static final Logger log = LoggerFactory.getLogger(KeywordController.class);

    private final KeywordService keywordService;
    private final RateLimitConfig rateLimitConfig;

    public KeywordController(KeywordService keywordService, RateLimitConfig rateLimitConfig) {
        this.keywordService = keywordService;
        this.rateLimitConfig = rateLimitConfig;
    }

    @GetMapping("/today")
    public ResponseEntity<?> getTodayKeywords(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "all") String source,
            @RequestParam(defaultValue = "all") String type,
            HttpServletRequest request
    ) {
        String clientIp = getClientIp(request);

        // Rate Limiting 체크
        if (!rateLimitConfig.tryConsume(clientIp)) {
            log.warn("Rate limit 초과: {}", clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", "60")
                    .body(new ErrorResponse("요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요."));
        }

        // 파라미터 검증
        if (limit < 1 || limit > 100) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("limit은 1-100 사이여야 합니다."));
        }

        if (!isValidSource(source)) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("source는 all, news, youtube 중 하나여야 합니다."));
        }

        if (!isValidType(type)) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("type은 all, PERSON, LOCATION, ORGANIZATION 중 하나여야 합니다."));
        }

        log.info("키워드 조회 요청: source={}, type={}, limit={}, ip={}", source, type, limit, clientIp);

        KeywordResponse response = keywordService.getKeywords(source, type, limit);
        return ResponseEntity.ok(response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isValidSource(String source) {
        return source.equals("all") || source.equals("news") || source.equals("youtube");
    }

    private boolean isValidType(String type) {
        return type.equals("all") || type.equals("PERSON")
                || type.equals("LOCATION") || type.equals("ORGANIZATION");
    }

    public record ErrorResponse(String message) {
    }
}
